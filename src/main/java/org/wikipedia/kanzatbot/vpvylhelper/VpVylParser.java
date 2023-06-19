package org.wikipedia.kanzatbot.vpvylhelper;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.fastily.jwiki.core.Wiki;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

                Pair<Integer, Integer> forAndAgainst = parseForAndAgainst(child.getContent().text);

                // Filter out subsections like "References"
                List<JWikiTree> subchildren = child.getChildren();
                if (subchildren.stream().anyMatch(c -> c.getHeader().equals("Підсумок"))) {
                    result.add(new PageDeletion(location, header, PageDeletionStatus.COMPLETED, forAndAgainst.getLeft(),
                            forAndAgainst.getRight()));
                } else if (subchildren.stream().anyMatch(c -> c.getHeader().equals("Оскаржений підсумок"))) {
                    result.add(new PageDeletion(location, header, PageDeletionStatus.CONTESTED, forAndAgainst.getLeft(),
                            forAndAgainst.getRight()));
                } else {
                    result.add(new PageDeletion(location, header, PageDeletionStatus.IN_PROGRESS,
                            forAndAgainst.getLeft(),
                            forAndAgainst.getRight()));
                }
            } catch (Exception e) {
                log.error("Failed to parse {}", header, e);
            }
        }
        return result;
    }

    private Pair<Integer, Integer> parseForAndAgainst(String content) {
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
        String approveText = content.substring(approveIndex + approveStartText.length(), rejectIndex);
        String rejectText = content.substring(rejectIndex + rejectStartText.length(), afterRejectIndex);
        Pattern pattern = Pattern.compile("(?m)^# ");
        Matcher matcherApprove = pattern.matcher(approveText);
        Matcher matcherReject = pattern.matcher(rejectText);
        return Pair.of((int) matcherApprove.results().count(), (int) matcherReject.results().count());
    }

}
