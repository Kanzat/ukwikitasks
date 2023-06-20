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

    public List<PageDeletion> getDeletions(final Wiki wiki, final String title) {
        log.info("Getting deleted articles for {}", title);
        Optional<JWikiTree> jWikiTree = MarkdownParser.parsePage(wiki, title);
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

}
