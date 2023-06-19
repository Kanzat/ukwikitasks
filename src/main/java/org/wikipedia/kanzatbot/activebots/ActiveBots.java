package org.wikipedia.kanzatbot.activebots;

import lombok.extern.slf4j.Slf4j;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.dwrap.Contrib;
import org.fastily.jwiki.dwrap.LogEntry;
import org.springframework.stereotype.Component;
import org.wikipedia.kanzatbot.jwiki.JWikiUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ActiveBots {


    public void generateReport(final Wiki ukWiki) {
        LocalDateTime end = LocalDate.now().atStartOfDay();
        LocalDateTime start = end.minus(30, ChronoUnit.DAYS);

        List<String> activeBots = JWikiUtils.getUsers(ukWiki, Collections.singletonList("bot"), true);
        log.debug("Found {} bots: {}", activeBots.size(), String.join(", ", activeBots));
        StringBuilder sb = new StringBuilder();
        sb.append("Список активних ботів за останні 30 днів, здійснені ними операції та редагування.\n");
        for (String activeBot : activeBots) {
            sb.append("== [[User:").append(activeBot).append("|").append(activeBot).append("]] ==\n");
            List<LogEntry> allLogs = JWikiUtils.getLogs(ukWiki, null, activeBot, null, -1, start, end);
            log.debug("Bot {} performed {} actions", activeBot, allLogs.size());
            Map<String, List<LogEntry>> logsByType = allLogs.stream().collect(Collectors.groupingBy(logEntry -> logEntry.type));
            for (Map.Entry<String, List<LogEntry>> entry : logsByType.entrySet()) {
                sb.append("* [https://uk.wikipedia.org/w/index.php?title=Special:Log&action=view&type=").append(entry.getKey()).append("&user=").append(activeBot).append(" ").append(entry.getKey()).append("] — ").append(entry.getValue().size()).append("\n");
            }

            List<Contrib> contribs = JWikiUtils.getContribs(ukWiki, activeBot, -1, false, false, start, end);
            Map<String, Integer> countSummaries = new HashMap<>();
            contribs.stream().map(contrib -> contrib.summary).forEach(s -> countSummaries.put(s,
                    countSummaries.getOrDefault(s, 0) + 1));
            sb.append("* [https://uk.wikipedia.org/wiki/Special:Contributions/").append(activeBot).append(" edits] — ").append(
                    contribs.size()).append("\n");
            Map<String, Integer> countSummariesSorted = sortByValue(countSummaries);
            int counter = 0;
            int countOthers = 0;
            for (Map.Entry<String, Integer> entry : countSummariesSorted.entrySet()) {
                if (++counter > 10) {
                    countOthers += entry.getValue();
                } else {
                    final String botComment = entry.getKey().isBlank() ? "''коментар відсутній''" :
                            ("<nowiki>" + entry.getKey() + "</nowiki>");
                    sb.append("*: ").append(entry.getValue()).append(" — ").append(botComment).append("\n");
                }
            }
            if (countOthers != 0) {
                sb.append("*: Інше — ").append(countOthers).append("\n");
            }
        }
        ukWiki.edit("User:KanzatBot/Reports/ActiveBots", sb.toString(), "Оновлення звіту");
    }


    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());

        Collections.sort(list, Map.Entry.<K, V>comparingByValue().reversed());

        Map<K, V> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

}
