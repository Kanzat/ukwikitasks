package org.wikipedia.kanzatbot.copyvio;

import lombok.extern.slf4j.Slf4j;
import org.fastily.jwiki.core.Wiki;
import org.springframework.stereotype.Component;
import org.wikipedia.kanzatbot.jwiki.JWikiUtils;
import org.wikipedia.kanzatbot.jwiki.RecentChangeEntry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import static java.util.stream.Collectors.toList;

@Slf4j
@Component
public class CopyvioDetector {

    private static Locale ukrainianLocale = new Locale("uk", "UA");
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", ukrainianLocale);

    public void runInNewPages(Wiki ukWiki) throws InterruptedException {
        LocalDateTime end = LocalDate.now().atStartOfDay();
        LocalDateTime start = end.minus(1, ChronoUnit.DAYS);
        List<RecentChangeEntry> recentChanges = JWikiUtils.getNewPages(ukWiki, start, end);
        log.info("За останню добу створено {} статей:", recentChanges.size());
        for (RecentChangeEntry recentChange : recentChanges) {
            log.info(recentChange.title);
        }

        // filter out redirect pages
        List<RecentChangeEntry> recentChangesFiltered = recentChanges.stream().filter(rc -> rc.redirect == null).collect(toList());

        StringBuilder report = new StringBuilder();
        for (RecentChangeEntry recentChange : recentChangesFiltered) {
            final String title = recentChange.title;
            final int maxRetries = 3;
            int retryCount = 0;
            CopyvioResultDto copyvio = null;
            boolean failed = false;
            while (retryCount < maxRetries) {
                try {
                    copyvio = CopyvioClient.getForTitle(title);
                    break;
                } catch (Exception e) {
                    failed = true;
                    log.error("Failed to get copyvio for page {}. Retrying...", title, e);
                    retryCount++;
                    Thread.sleep(2 * 60000L * retryCount);
                }
            }

            if (failed) {
                report.append("* [[").append(title).append("]] — failed").append("\n");
            } else if (copyvio == null) {
                log.error("Not possible");
            } else if (copyvio.best != null &&
                    (copyvio.best.violation.equals("suspected") || copyvio.best.violation.equals("possible")) &&
                    copyvio.best.confidence > 0
            ) {
                log.info("Article {} is likely to have issues with copyvio", title);
                report.append("* [[").append(title).append("]] — violation: ").append(copyvio.best.violation)
                        .append(", confidence: ").append(copyvio.best.confidence).append("\n");
            } else {
                log.info("Article {} is likely to be ok", title);
            }
        }

        final String reportTitle = "User:KanzatBot/Reports/Copyvio";
        final String originalPageText = ukWiki.getPageText(reportTitle);
        String newPageText = "== " + formatter.format(start) + " ==\n";
        newPageText += report.toString();
        newPageText += "\n";
        newPageText += originalPageText;
        ukWiki.edit(reportTitle, newPageText, "upd");
    }

}
