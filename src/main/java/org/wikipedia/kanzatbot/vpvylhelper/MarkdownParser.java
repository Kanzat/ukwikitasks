package org.wikipedia.kanzatbot.vpvylhelper;

import lombok.extern.slf4j.Slf4j;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.dwrap.PageSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class MarkdownParser {

    public static Optional<JWikiTree> parsePage(Wiki wiki, String title) {
        log.info("Parsing page {}", title);
        List<PageSection> pageSections = wiki.splitPageByHeader(title);
        if (pageSections.isEmpty()) {
            return Optional.empty();
        }
        JWikiTree root = new JWikiTree(null, new ArrayList<>(), null);
        for (PageSection pageSection : pageSections) {
            if (pageSection.level == -1) {
                if (root.content != null) {
                    throw new IllegalStateException("Unexpected: 2 root contents");
                }
                root.content = pageSection;
                continue;
            }
            JWikiTree current = root;
            while (true) {
                if (current.getLastChildren() == null || current.getLastChildren().getLevel() >= pageSection.level) {
                    break;
                }
                current = current.getLastChildren();
            }
            current.children.add(new JWikiTree(current, new ArrayList<>(), pageSection));
        }
        return Optional.of(root);
    }

}
