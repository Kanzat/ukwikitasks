package org.wikipedia.kanzatbot.protect;

import lombok.extern.slf4j.Slf4j;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.dwrap.LogEntry;
import org.springframework.stereotype.Component;
import org.wikipedia.kanzatbot.jwiki.EditEntry;
import org.wikipedia.kanzatbot.jwiki.JWikiUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Comparator.comparing;

@Slf4j
@Component
public class ProtectDetector {

    private static Locale ukrainianLocale = new Locale("uk", "UA");
    private static DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", ukrainianLocale);
    private static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy', 'HH:mm:ss", ukrainianLocale);
    private static int MIN_ROLLBACK_COUNT = 2;

    public void detect(Wiki wiki) {
        LocalDateTime end = LocalDate.now().atStartOfDay();
        LocalDateTime start = end.minus(30, ChronoUnit.DAYS);
        List<EditEntry> rollbackEdits = JWikiUtils.getEdits(wiki, "mw-rollback", start, end, null);
        // List<RecentChangeEntry> manualRevertEdits = JWikiUtils.getEdits(wiki, "mw-manual-revert", start, end);
        // List<RecentChangeEntry> undoEdits = JWikiUtils.getEdits(wiki, "mw-undo", start, end);
        List<EditEntry> filteredRollbackEdits = new ArrayList<>();
        for (EditEntry rollbackEdit : rollbackEdits) {
            if (rollbackEdit.comment != null && rollbackEdit.comment.contains("Відкинуто редагування [[Special:Contributions/" + rollbackEdit.user + "|")) {
                log.info("Ignore self rollback in {} by {} with comment {}", rollbackEdit.title, rollbackEdit.user, rollbackEdit.comment);
            } else {
                filteredRollbackEdits.add(rollbackEdit);
            }
        }

        LocalDateTime previousDay = LocalDateTime.now().minus(1, ChronoUnit.DAYS);
        StringBuilder report = new StringBuilder();
        boolean reportIsEmpty = true;
        report.append("== ").append(dateFormatter.format(previousDay)).append(" ==\n");
        Map<String, List<EditEntry>> editsByTitle = Stream.of(filteredRollbackEdits) //, manualRevertEdits, undoEdits)
                .flatMap(Collection::stream).sorted(comparing(recentChangeEntry -> recentChangeEntry.timestamp)).collect(Collectors.groupingBy(recentChangeEntry -> recentChangeEntry.title));
        for (Map.Entry<String, List<EditEntry>> entry : editsByTitle.entrySet()) {
            if (entry.getValue().size() < MIN_ROLLBACK_COUNT) {
                continue;
            }
            String articleTitle = entry.getKey();
            List<EditEntry> articleEdits = JWikiUtils.getEdits(wiki, null, start, end, articleTitle);
            List<EditEntry> articleRollbackEdits = entry.getValue();
            List<EditEntry> articleRollbackedEdits = new ArrayList<>();
            for (EditEntry articleRollbackEdit : articleRollbackEdits) {
                Optional<EditEntry> rollbackedEdit = findRollbackedEdit(articleEdits, articleRollbackEdit);
                if (rollbackedEdit.isEmpty()) {
                    continue;
                }
                // Only count edits done by anonymous users or hidden users
                boolean isAnonymousOrVandal = rollbackedEdit.get().userhidden != null || rollbackedEdit.get().userid == 0;
                if (isAnonymousOrVandal) {
                    articleRollbackedEdits.add(rollbackedEdit.get());
                }
            }

            if (articleRollbackedEdits.size() < MIN_ROLLBACK_COUNT) {
                continue;
            }

            EditEntry lastRollbackEntry = findLastEdit(articleRollbackEdits);
            EditEntry lastRollbackedEntry = findLastEdit(articleRollbackedEdits);

            if (lastRollbackEntry.getTimestamp().isBefore(previousDay.toInstant(ZoneOffset.UTC))) {
                // Ignore old wars
                continue;
            }

            report.append("* [[").append(articleTitle).append("]] (").append(articleRollbackedEdits.size()).append(" рази/разів):\n");
            articleRollbackedEdits.forEach(v -> report.append("*: ").append(v.user).append(", час: ").append(formatTime(v.timestamp)).append("\n"));

            Optional<LogEntry> lastProtectEntryOpt = getLastProtectEntry(wiki, articleTitle);
            boolean isProtected = lastProtectEntryOpt.isPresent() && lastProtectEntryOpt.get().timestamp.isAfter(lastRollbackedEntry.timestamp);
            if (isProtected) {
                LogEntry lastProtectEntry = lastProtectEntryOpt.get();
                report.append(format("*: '''Захищено''' %s, час: %s%n", lastProtectEntry.user,
                        formatTime(lastProtectEntry.timestamp)));
            }

            boolean requestExists = wiki.whatLinksHere(articleTitle).stream().anyMatch(lh -> lh.equals("Вікіпедія:Захист сторінок"));
            report.append("*: [[Вікіпедія:Захист сторінок|Запит на захист]]");
            if (!requestExists) {
                report.append(" '''НЕ'''");
            }
            report.append(" створено\n");

            reportIsEmpty = false;
        }

        if (!reportIsEmpty) {
            String reportTitle = "User:KanzatBot/Reports/ProtectDetector";
            String existingPageText = wiki.getPageText(reportTitle);
            wiki.edit(reportTitle, report + "\n" + existingPageText, "upd");
        }
    }

    private Optional<LogEntry> getLastProtectEntry(Wiki wiki, String title) {
        List<LogEntry> articleLogs = wiki.getLogs(title, null, "protect", -1);
        if (!articleLogs.isEmpty()) {
            return Optional.ofNullable(articleLogs.get(0));
        }
        return Optional.empty();
    }

    private EditEntry findLastEdit(List<EditEntry> edits) {
        return edits.stream().max(comparing(EditEntry::getTimestamp)).get();
    }

    /**
     * RollbackED edit is the edit right after rollback edit
     */
    private Optional<EditEntry> findRollbackedEdit(List<EditEntry> lastArticleEdits, EditEntry rollbackEdit) {
        boolean found = false;
        for (EditEntry edit : lastArticleEdits) {
            if (found) {
                return Optional.of(edit);
            }
            if (edit.revid == rollbackEdit.revid) {
                found = true;
            }
        }
        return Optional.empty();
    }

    private static String formatTime(Instant timestamp) {
        return dateTimeFormatter.format(LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault()));
    }

}
