package org.wikipedia.kanzatbot.vpvylhelper;

import lombok.extern.slf4j.Slf4j;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.dwrap.PageSection;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Slf4j
@Component
public class VpVylParser {

    /**
     * Pauses between attempts to load a page that exists but could not be loaded.
     */
    static final long[] RETRY_DELAYS_MS = {5_000, 15_000, 30_000};

    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    Sleeper sleeper = Thread::sleep;

    public List<PageDeletion> getDeletions(final Wiki wiki, final String title) {
        log.info("Getting deleted articles for {}", title);
        Optional<JWikiTree> jWikiTree = parsePageWithRetry(wiki, title);
        if (jWikiTree.isEmpty()) {
            log.info("Page {} does not exist", title);
            return new ArrayList<>();
        }
        List<PageDeletion> pageDeletions = convertToPageDeletion(title, jWikiTree.get());
        for (PageDeletion pageDeletion : pageDeletions) {
            log.debug("Found {} with status {}", pageDeletion.title, pageDeletion.status);
        }
        return pageDeletions;
    }

    /**
     * jwiki's splitPageByHeader swallows any API or network error and returns an empty list, which is
     * indistinguishable from a missing page. Treating a failed load as "page does not exist" silently dropped most
     * days from the DeletionStats report on 2026-10-28. So an empty result is checked with exists() and the load is
     * retried; if the page exists but still can't be loaded, this fails loudly instead of returning partial data.
     */
    private Optional<JWikiTree> parsePageWithRetry(final Wiki wiki, final String title) {
        for (int attempt = 0; ; attempt++) {
            Optional<JWikiTree> tree = MarkdownParser.parsePage(wiki, title);
            if (tree.isPresent()) {
                return tree;
            }
            Boolean exists = null;
            try {
                exists = wiki.exists(title);
            } catch (Exception e) {
                // jwiki's exists() throws NullPointerException when the request fails
                log.warn("Failed to check existence of {}", title, e);
            }
            if (Boolean.FALSE.equals(exists)) {
                return Optional.empty();
            }
            if (attempt >= RETRY_DELAYS_MS.length) {
                throw new IllegalStateException("Failed to load page " + title + " after " + (attempt + 1) + " attempts");
            }
            log.warn("Page {} could not be loaded (exists={}), retrying in {} ms", title, exists, RETRY_DELAYS_MS[attempt]);
            try {
                sleeper.sleep(RETRY_DELAYS_MS[attempt]);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while loading page " + title, e);
            }
        }
    }

    private List<PageDeletion> convertToPageDeletion(String location, JWikiTree tree) {
        List<PageDeletion> result = new ArrayList<>();
        for (JWikiTree child : tree.getChildren()) {
            final int level = child.getLevel();
            String header = child.getHeader().trim();
            try {
                if (header.startsWith("<s>") && header.endsWith("</s>")) {
                    header = header.substring("<s>".length(), header.length() - "</s>".length()).trim();
                }
                log.debug("Analyzing {}", header);
                if (level != 2) {
                    throw new IllegalStateException("Child " + header + " is not of level 2, but was " + level);
                }

                // Filter out subsections like "References"
                List<JWikiTree> subchildren = child.getChildren();
                PageDeletion.PageDeletionBuilder pdb = PageDeletion.builder().location(location).title(header);
                PageSection summarySection =
                        subchildren.stream().filter(c -> c.getHeader().equals("Підсумок")).findFirst().map(JWikiTree::getContent).orElse(null);
                PageSection contestedSummarySection =
                        subchildren.stream().filter(c -> c.getHeader().equals("Оскаржений підсумок")).findFirst().map(JWikiTree::getContent).orElse(null);
                pdb.finalSummaryAdmin(parseSummary(summarySection));
                pdb.contestedSummaryAdmin(parseSummary(contestedSummarySection));
                pdb.finalSummaryStatus(parseSummaryStatus(summarySection));
                pdb.contestedSummaryStatus(parseSummaryStatus(contestedSummarySection));
                if (summarySection != null) {
                    pdb.status(PageDeletionStatus.COMPLETED);
                } else if (contestedSummarySection != null) {
                    pdb.status(PageDeletionStatus.CONTESTED);
                } else {
                    pdb.status(PageDeletionStatus.IN_PROGRESS);
                }

                parseMainText(child.getContent().text, pdb);

                result.add(pdb.build());
            } catch (Exception e) {
                log.error("Failed to parse {}", header, e);
            }
        }
        return result;
    }

    String parseSummary(PageSection summarySection) {
        if (summarySection == null) {
            return null;
        }
        return parseSummary(summarySection.text);
    }

    String parseSummary(String text) {
        String firstNonHeaderLine =
                Arrays.stream(text.split("\n")).filter(s -> !s.trim().isBlank()).skip(1).findFirst().orElse(null);
        if (firstNonHeaderLine == null) {
            return null;
        }
        return getSignatureUser(firstNonHeaderLine).orElse(null);
    }

    private void parseMainText(String content, PageDeletion.PageDeletionBuilder pdb) {
        String approveStartText = "* {{За}}:";
        String rejectStartText = "* {{Проти}}:";
        String afterRejectText = "* {{";
        int approveIndex = content.indexOf(approveStartText);
        int rejectIndex = content.indexOf(rejectStartText, approveIndex + approveStartText.length());
        int afterRejectIndex = content.indexOf(afterRejectText, rejectIndex + rejectStartText.length());
        if (approveIndex == -1) {
            throw new IllegalStateException("Unable to find approve start");
        }
        if (rejectIndex == -1) {
            throw new IllegalStateException("Unable to find reject start");
        }
        if (afterRejectIndex == -1) {
            throw new IllegalStateException("Unable to find after-reject start");
        }
        String nominatedByLine =
                Arrays.stream(content.split("\n")).filter(l -> l.startsWith("* '''Постави")).findFirst().orElseThrow(() -> new IllegalStateException("Unable to find nominated-by start"));
        String approveText = content.substring(approveIndex + approveStartText.length(), rejectIndex);
        String rejectText = content.substring(rejectIndex + rejectStartText.length(), afterRejectIndex);
        pdb.nominatedBy(getSignatureUser(nominatedByLine).orElse(null)).votedApprove(getVoters(approveText)).votedReject(getVoters(rejectText));
    }

    private List<String> getVoters(String text) {
        return Arrays.stream(text.split("\n")).filter(l -> l.startsWith("# ")).map(this::getSignatureUser).map(o -> o.orElse(null)).collect(toList());
    }

    private Optional<String> getSignatureUser(String line) {
        final List<String> endings = Stream.of("[[Користувач:", "[[Користувачка:", "[[User:", "[[User talk:",
                "[[Обговорення користувача:", "[[Обговорення користувачки:", "[[Спеціальна:Внесок/").collect(toList());
        Map<Integer, String> positionToText = new HashMap<>();
        for (String ending : endings) {
            positionToText.put(line.lastIndexOf(ending), ending);
        }
        final int userIndex = positionToText.keySet().stream().max(Integer::compareTo).get();
        final String userText = positionToText.get(userIndex);
        if (userIndex == -1) {
            log.warn("Failed to find signature in line {}", line);
            return Optional.empty();
        }
        int endIndex = line.indexOf("|", userIndex + userText.length());
        if (endIndex == -1) {
            log.warn("Failed to find signature in line {}", line);
            return Optional.empty();
        }
        return Optional.of(line.substring(userIndex + userText.length(), endIndex));
    }

    private PageDeletionCompletedStatus parseSummaryStatus(PageSection summarySection) {
        if (summarySection == null) {
            return null;
        }
        String summaryText = summarySection.text.toLowerCase(Locale.ROOT);
        if (summaryText.contains("вилучено") || summaryText.contains("вилучена") || summaryText.contains("сторінку вилучив адміністратор")) {
            return PageDeletionCompletedStatus.DELETED;
        } else if (summaryText.contains("залишено") || summaryText.contains("залишена") || summaryText.contains("перейменовано")) {
            return PageDeletionCompletedStatus.KEPT;
        } else if (summaryText.contains("перенаправлено") || summaryText.contains("створено перенаправлення")
                || summaryText.contains("інформацію перенесено до") || summaryText.contains("замінено на перенаправлення")
                || summaryText.contains("замінена перенаправленням на")) {
            return PageDeletionCompletedStatus.DELETED_WITH_REDIRECT;
        } else if (summaryText.contains("знімаю номінацію")) {
            return PageDeletionCompletedStatus.UNNOMINATED;
        } else {
            return PageDeletionCompletedStatus.UNKNOWN;
        }
    }

}
