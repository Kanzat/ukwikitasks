package org.wikipedia.kanzatbot.copyviopages;

import lombok.extern.slf4j.Slf4j;
import org.fastily.jwiki.core.NS;
import org.fastily.jwiki.core.WParser;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.dwrap.Revision;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

        Map<LocalDate, List<String>> pagesByDate = new HashMap<>();
        for (String copyvioPage : copyvioPages) {
            waitALittle();
            Revision earliestCopyvioRevision = findRevisionThatAddedTemplate(wiki, copyvioPage);
            if (earliestCopyvioRevision == null) {
                log.error("Failed to find copyvio revision");
                continue;
            }
            LocalDate date = earliestCopyvioRevision.timestamp.atOffset(ZoneOffset.UTC).toLocalDate();
            pagesByDate.computeIfAbsent(date, d -> new ArrayList<>()).add(copyvioPage);
            log.info(copyvioPage + " from " + earliestCopyvioRevision.timestamp);
        }

        final String page = renderPage(pagesByDate, LocalDate.now(ZoneOffset.UTC));
        final boolean success = wiki.edit(targetPageName, page, "Автоматичне оновлення інформації");
        log.info("Successful operation? " + success);
    }

    /**
     * The revision that added the template: the oldest one of the uninterrupted run of revisions with the template,
     * counting from the newest. Null if the newest revision has no template.
     */
    static Revision findRevisionThatAddedTemplate(Wiki wiki, String page) {
        List<Revision> lastRevisions = wiki.getRevisions(page, 10, false, null, null);
        Revision earliest = earliestWithTemplate(wiki, lastRevisions);
        if (earliest != null && earliest == lastRevisions.get(lastRevisions.size() - 1)) {
            // The template is in all the last 10 revisions and may be older, look through the whole history
            earliest = earliestWithTemplate(wiki, wiki.getRevisions(page, 0, false, null, null));
        }
        return earliest;
    }

    private static Revision earliestWithTemplate(Wiki wiki, List<Revision> newestFirst) {
        Revision earliest = null;
        for (Revision revision : newestFirst) {
            if (!hasCopyvioTemplate(wiki, revision)) {
                break;
            }
            earliest = revision;
        }
        return earliest;
    }

    static String renderPage(Map<LocalDate, ? extends Collection<String>> pagesByDate, LocalDate today) {
        StringBuilder sb = new StringBuilder();
        LocalDate purgeDate = today.minusDays(7);
        boolean purgeDateUsed = false;
        sb.append("{{/шапка}}\n\n");
        List<LocalDate> dates = pagesByDate.keySet().stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());
        for (LocalDate date : dates) {
            if (!purgeDateUsed && purgeDate.isAfter(date)) {
                sb.append("{{").append(RESOLUTION_TEMPLATE).append("/Message}}\n\n");
                purgeDateUsed = true;
            }
            String pagesStr = pagesByDate.get(date).stream().sorted().map(p -> "[[" + p + "]]").collect(Collectors.joining(" • "));
            sb.append("{{").append(RESOLUTION_TEMPLATE).append("|").append(date.format(DATE_FORMATTER)).append("|").append(pagesStr).append("}}\n\n");
        }
        sb.append("{{/низ}}\n\n");
        return sb.toString();
    }

    private static boolean hasCopyvioTemplate(Wiki wiki, Revision revision) {
        String text = revision.text != null ? revision.text : "";
        if (containsCopyvioTemplate(text)) {
            return true;
        }
        // Fall back to the parser for markup the plain text search misses, e.g. "{{ Copyvio"
        WParser.WikiText wikiText = WParser.parseText(wiki, text);
        return wikiText.getTemplates().stream().anyMatch(t -> COPYVIO_TEMPLATES.contains(normalizeText(t.title)));
    }

    static boolean containsCopyvioTemplate(String text) {
        String normalizedText = normalizeText(text);
        return COPYVIO_TEMPLATES.stream().anyMatch(t -> normalizedText.contains("{{" + t));
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
