/*
 * Decompiled with CFR 0.152.
 */
package org.wikipedia.kanzatbot.copyviopages;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.fastily.jwiki.core.NS;
import org.fastily.jwiki.core.WParser;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.dwrap.Revision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CopyvioPages {
    private static final Logger logger = LoggerFactory.getLogger(CopyvioPages.class);

    public static void update(Wiki wiki, String targetPageName) {
        ArrayList<String> copyvioPages = wiki.getCategoryMembers("\u041a\u0430\u0442\u0435\u0433\u043e\u0440\u0456\u044f:\u0412\u0456\u043a\u0456\u043f\u0435\u0434\u0456\u044f:\u041c\u043e\u0436\u043b\u0438\u0432\u0435 \u043f\u043e\u0440\u0443\u0448\u0435\u043d\u043d\u044f \u0430\u0432\u0442\u043e\u0440\u0441\u044c\u043a\u0438\u0445 \u043f\u0440\u0430\u0432", new NS[0]);
        copyvioPages.removeIf(cp -> wiki.whichNS((String)cp).equals(NS.CATEGORY));
        TreeMap copyvioMap = new TreeMap(Collections.reverseOrder());
        for (String copyvioPage : copyvioPages) {
            CopyvioPages.waitALittle();
            ArrayList<Revision> copyvioRevisions = wiki.getRevisions(copyvioPage, 10, false, null, null);
            Object earliestCopyvioRevision = null;
            boolean breakNotInvoked = true;
            for (Revision revision : copyvioRevisions) {
                if (CopyvioPages.hasCopyvioTemplate(wiki, revision)) {
                    earliestCopyvioRevision = revision;
                    continue;
                }
                breakNotInvoked = false;
                break;
            }
            if (breakNotInvoked) {
                ArrayList<Revision> copyvioRevisionsAgain = wiki.getRevisions(copyvioPage, 0, false, null, null);
                for (Revision copyvioRevision : copyvioRevisionsAgain) {
                    if (!CopyvioPages.hasCopyvioTemplate(wiki, copyvioRevision)) break;
                    earliestCopyvioRevision = copyvioRevision;
                }
            }
            if (earliestCopyvioRevision != null) {
                LocalDate localDate = ((Revision)earliestCopyvioRevision).timestamp.atOffset(ZoneOffset.UTC).toLocalDate();
                Set set = copyvioMap.computeIfAbsent(localDate, instant -> new TreeSet());
                set.add(copyvioPage);
                logger.info(copyvioPage + " from " + ((Revision)earliestCopyvioRevision).timestamp);
                continue;
            }
            logger.error("Failed to find copyvio revision");
        }
        StringBuilder sb = new StringBuilder();
        LocalDate purgeDate = Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().minusDays(7L);
        boolean purgeDateUsed = false;
        sb.append("{{/\u0448\u0430\u043f\u043a\u0430}}\n\n");
        for (Map.Entry localDateListEntry : copyvioMap.entrySet()) {
            LocalDate date = (LocalDate)localDateListEntry.getKey();
            if (!purgeDateUsed && purgeDate.isAfter(date)) {
                sb.append("{{\u0412\u0438\u0440\u0456\u0448\u0435\u043d\u043d\u044f \u043f\u0440\u043e\u0431\u043b\u0435\u043c \u0430\u0432\u0442\u043e\u0440\u0441\u044c\u043a\u043e\u0433\u043e \u043f\u0440\u0430\u0432\u0430/Message}}\n\n");
                purgeDateUsed = true;
            }
            Set set = (Set)localDateListEntry.getValue();
            String formattedDate = date.format(DateTimeFormatter.ofPattern("yyyy-M-d"));
            String pagesStr = set.stream().map(p -> "[[" + p + "]]").collect(Collectors.joining(" \u2022 "));
            sb.append("{{\u0412\u0438\u0440\u0456\u0448\u0435\u043d\u043d\u044f \u043f\u0440\u043e\u0431\u043b\u0435\u043c \u0430\u0432\u0442\u043e\u0440\u0441\u044c\u043a\u043e\u0433\u043e \u043f\u0440\u0430\u0432\u0430|").append(formattedDate).append("|").append(pagesStr).append("}}\n\n");
        }
        sb.append("{{/\u043d\u0438\u0437}}\n\n");
        boolean success = wiki.edit(targetPageName, sb.toString(), "\u0410\u0432\u0442\u043e\u043c\u0430\u0442\u0438\u0447\u043d\u0435 \u043e\u043d\u043e\u0432\u043b\u0435\u043d\u043d\u044f \u0456\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0456\u0457");
        logger.info("Successful operation? " + success);
    }

    private static boolean hasCopyvioTemplate(Wiki wiki, Revision copyvioRevision) {
        String text = copyvioRevision.text != null ? copyvioRevision.text : "";
        String normalizedText = CopyvioPages.normalizeText(text);
        if (normalizedText.contains("{{\u043f\u043e\u0440\u0443\u0448\u0435\u043d\u043d\u044f \u0430\u0432\u0442\u043e\u0440\u0441\u044c\u043a\u0438\u0445 \u043f\u0440\u0430\u0432") || normalizedText.contains("{{\u043a\u043e\u043f\u0456\u0432\u043e") || normalizedText.contains("{{copyvio") || normalizedText.contains("{{\u0430\u0432\u0442\u043e\u0440\u0441\u044c\u043a\u0456 \u043f\u0440\u0430\u0432\u0430") || normalizedText.contains("{{\u0434\u043e\u0441\u043b\u0456\u0432\u043d\u0438\u0439 \u043f\u0435\u0440\u0435\u043a\u043b\u0430\u0434")) {
            return true;
        }
        CopyvioPages.waitALittle();
        WParser.WikiText wikiText = WParser.parseText(wiki, text);
        for (WParser.WTemplate template : wikiText.getTemplates()) {
            String templateTitleNormalized = CopyvioPages.normalizeText(template.title);
            if (!templateTitleNormalized.equals("\u043f\u043e\u0440\u0443\u0448\u0435\u043d\u043d\u044f \u0430\u0432\u0442\u043e\u0440\u0441\u044c\u043a\u0438\u0445 \u043f\u0440\u0430\u0432") && !templateTitleNormalized.equals("\u043a\u043e\u043f\u0456\u0432\u043e") && !templateTitleNormalized.equals("copyvio") && !templateTitleNormalized.equals("\u0430\u0432\u0442\u043e\u0440\u0441\u044c\u043a\u0456 \u043f\u0440\u0430\u0432\u0430") && !templateTitleNormalized.equals("\u0434\u043e\u0441\u043b\u0456\u0432\u043d\u0438\u0439 \u043f\u0435\u0440\u0435\u043a\u043b\u0430\u0434")) continue;
            return true;
        }
        return false;
    }

    private static String normalizeText(String text) {
        return text.replace("_", " ").toLowerCase(Locale.ROOT);
    }

    public static void waitALittle() {
        try {
            Thread.sleep(1000L);
        }
        catch (InterruptedException interruptedException) {
            // empty catch block
        }
    }
}

