package org.wikipedia.kanzatbot.copyviopages;

import lombok.extern.slf4j.Slf4j;
import org.fastily.jwiki.core.NS;
import org.fastily.jwiki.core.WParser;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.dwrap.Revision;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CopyvioPages {

    private static final String COPYVIO_CATEGORY = "Категорія:Вікіпедія:Можливе порушення авторських прав";
    private static final String RESOLUTION_TEMPLATE = "Вирішення проблем авторського права";
    // Lower case, as compared after normalizeText
    private static final List<String> COPYVIO_TEMPLATES = List.of(
            "порушення авторських прав", "копіво", "copyvio", "авторські права", "дослівний переклад");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-M-d");

    public void update(Wiki wiki, String targetPageName) {
        List<String> copyvioPages = wiki.getCategoryMembers(COPYVIO_CATEGORY);
        copyvioPages.removeIf(cp -> wiki.whichNS(cp).equals(NS.CATEGORY));

        Map<LocalDate, Set<String>> copyvioMap = new TreeMap<>(Collections.reverseOrder());
        for (String copyvioPage : copyvioPages) {
            waitALittle();
            Revision earliestCopyvioRevision = null;
            boolean allHaveTemplate = true;
            for (Revision revision : wiki.getRevisions(copyvioPage, 10, false, null, null)) {
                if (!hasCopyvioTemplate(wiki, revision)) {
                    allHaveTemplate = false;
                    break;
                }
                earliestCopyvioRevision = revision;
            }
            if (allHaveTemplate) {
                // The template may be older than the last 10 revisions, look through the whole history
                for (Revision revision : wiki.getRevisions(copyvioPage, 0, false, null, null)) {
                    if (!hasCopyvioTemplate(wiki, revision)) {
                        break;
                    }
                    earliestCopyvioRevision = revision;
                }
            }
            if (earliestCopyvioRevision == null) {
                log.error("Failed to find copyvio revision");
                continue;
            }
            LocalDate date = earliestCopyvioRevision.timestamp.atOffset(ZoneOffset.UTC).toLocalDate();
            copyvioMap.computeIfAbsent(date, d -> new TreeSet<>()).add(copyvioPage);
            log.info(copyvioPage + " from " + earliestCopyvioRevision.timestamp);
        }

        StringBuilder sb = new StringBuilder();
        LocalDate purgeDate = Instant.now().atOffset(ZoneOffset.UTC).toLocalDate().minusDays(7);
        boolean purgeDateUsed = false;
        sb.append("{{/шапка}}\n\n");
        for (Map.Entry<LocalDate, Set<String>> entry : copyvioMap.entrySet()) {
            LocalDate date = entry.getKey();
            if (!purgeDateUsed && purgeDate.isAfter(date)) {
                sb.append("{{").append(RESOLUTION_TEMPLATE).append("/Message}}\n\n");
                purgeDateUsed = true;
            }
            String pagesStr = entry.getValue().stream().map(p -> "[[" + p + "]]").collect(Collectors.joining(" • "));
            sb.append("{{").append(RESOLUTION_TEMPLATE).append("|").append(date.format(DATE_FORMATTER)).append("|").append(pagesStr).append("}}\n\n");
        }
        sb.append("{{/низ}}\n\n");
        final boolean success = wiki.edit(targetPageName, sb.toString(), "Автоматичне оновлення інформації");
        log.info("Successful operation? " + success);
    }

    private static boolean hasCopyvioTemplate(Wiki wiki, Revision revision) {
        String text = revision.text != null ? revision.text : "";
        String normalizedText = normalizeText(text);
        if (COPYVIO_TEMPLATES.stream().anyMatch(t -> normalizedText.contains("{{" + t))) {
            return true;
        }
        // Fall back to the parser for markup the plain text search misses, e.g. "{{ Copyvio"
        waitALittle();
        WParser.WikiText wikiText = WParser.parseText(wiki, text);
        return wikiText.getTemplates().stream().anyMatch(t -> COPYVIO_TEMPLATES.contains(normalizeText(t.title)));
    }

    private static String normalizeText(String text) {
        return text.replace("_", " ").toLowerCase(Locale.ROOT);
    }

    private static void waitALittle() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
