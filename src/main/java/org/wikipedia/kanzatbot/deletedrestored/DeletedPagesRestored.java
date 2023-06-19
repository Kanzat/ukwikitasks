package org.wikipedia.kanzatbot.deletedrestored;

import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.dwrap.LogEntry;
import org.springframework.stereotype.Component;
import org.wikipedia.kanzatbot.jwiki.JWikiUtils;
import org.wikipedia.kanzatbot.jwiki.RecentChangeEntry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@Slf4j
@Component
public class DeletedPagesRestored {

    private static Locale ukrainianLocale = new Locale("uk", "UA");
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", ukrainianLocale);

    public void find(Wiki wiki) {
        LocalDateTime end = LocalDate.now().atStartOfDay();
        LocalDateTime start = end.minus(1, ChronoUnit.DAYS);
        List<RecentChangeEntry> newPages = JWikiUtils.getNewPages(wiki, start, end);

        List<RecentChangeEntry> restoredPages = new ArrayList<>();
        for (RecentChangeEntry newPage : newPages) {
            log.debug("Getting logs for {}", newPage.title);
            List<LogEntry> logs = wiki.getLogs(newPage.title, null, null, -1);
            List<LogEntry> filteredLogs = logs.stream().filter(l -> l.type.equals("create") || l.type.equals("delete")).collect(toList());
            if (filteredLogs.isEmpty()) {
                log.warn("No create/delete events for page {}", newPage.title);
                continue;
            }
            LogEntry lastLogEntry = filteredLogs.get(0);
            if (lastLogEntry.type.equals("delete")) {
                log.info("Skipping {}, because it's already deleted", newPage.title);
            } else if (lastLogEntry.type.equals("create")) {
                if (filteredLogs.stream().anyMatch(l -> l.type.equals("delete"))) {
                    log.info("Page {} was previously deleted and now it's restored", newPage.title);
                    restoredPages.add(newPage);
                }
            } else {
                log.error("Illegal type {}", lastLogEntry.type);
            }
        }

        log.info("Found {} restored pages", restoredPages.size());

        StringBuilder report = new StringBuilder();
        report.append("{| class=\"wikitable\"\n");
        report.append("|-\n");
        report.append("! Стаття !! Редирект !! Попередні вилучення !! Попередні відновлення !! Журнал \n");
        report.append("|-\n");
        for (RecentChangeEntry restoredPage : restoredPages) {
            List<String> linksHere = wiki.whatLinksHere(restoredPage.title);
            List<String> linksToVpVyl = linksHere.stream().filter(lh -> lh.startsWith("Вікіпедія:Статті-кандидати на вилучення/")).collect(toList());
            List<String> linksToVpVvs = linksHere.stream().filter(lh -> lh.startsWith("Вікіпедія:Відновлення вилучених сторінок")).collect(toList());
            String article = makeLink(restoredPage.title);
            String redirect = restoredPage.redirect != null ? "Так" : "Ні";
            String vpvyl = linksToVpVyl.stream().map(DeletedPagesRestored::makeLink).collect(joining("<br/>"));
            vpvyl = vpvyl.isEmpty() ? "—" : vpvyl;
            String vpvvs = linksToVpVvs.stream().map(DeletedPagesRestored::makeLink).collect(joining("<br/>"));
            vpvvs = vpvvs.isEmpty() ? "—" : vpvvs;
            String journalLink = "[" + new HttpUrl.Builder().scheme("https")
                    .host("uk.wikipedia.org").addPathSegment("wiki/Спеціальна:Журнали")
                    .addQueryParameter("page", restoredPage.title).build().url().toExternalForm() + " тут]";
            report.append("| " + article + " || " + redirect + " || " + vpvyl + " || " + vpvvs + " || " + journalLink + "\n");
            report.append("|-\n");
        }
        report.append("|}\n");

        final String reportTitle = "User:KanzatBot/Reports/RestoredPages";
        final String originalPageText = wiki.getPageText(reportTitle);
        String newPageText = "== " + formatter.format(start) + " ==\n";
        newPageText += report.toString();
        newPageText += "\n";
        newPageText += originalPageText;
        wiki.edit(reportTitle, newPageText, "upd");
    }

    private static String makeLink(final String title) {
        return "[[" + title + "]]";
    }

}
