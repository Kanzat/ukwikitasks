package org.wikipedia.kanzatbot.vpvylhelper;

import lombok.extern.slf4j.Slf4j;
import org.fastily.jwiki.core.Wiki;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Slf4j
@Component
public class VpVylStats {

    @Autowired
    VpVylParser vpVylParser;

    public void generateReport(Wiki wiki) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("uk", "UA"));
        LocalDate end = LocalDate.now();
        LocalDate start = end.minus(365, ChronoUnit.DAYS);

        List<PageDeletion> allDeletions = new ArrayList<>();
        while (start.isBefore(end)) {
            String title = "Вікіпедія:Статті-кандидати на вилучення/" + formatter.format(start);
            List<PageDeletion> deletions = vpVylParser.getDeletions(wiki, title);
            for (PageDeletion deletion : deletions) {
                log.info(deletion.toString());
            }
            allDeletions.addAll(deletions);
            start = start.plus(1, ChronoUnit.DAYS);
        }

        StringBuilder report = new StringBuilder();
        report.append("Звіт про діяльність номінаторів та адміністраторів на [[ВП:ВИЛ]] за останні 365 днів.\n");
        displayNominators(allDeletions, report);
        displaySummarizers(allDeletions, report);

        wiki.edit("User:KanzatBot/Reports/DeletionStats", report.toString(), "Оновлення статистики про ВП:ВИЛ");
    }

    private void displayNominators(List<PageDeletion> allDeletions, StringBuilder report) {
        report.append("== Номінатори ==\n");
        report.append("{| class=\"wikitable sortable\"\n");
        report.append("|-\n");
        report.append("! Номінатор !! Кількість номінацій !! З підсумком (%) !! Вилучено (%) !! Залишено (%) \n");
        report.append("|-\n");
        Map<String, List<PageDeletion>> deletionsByNominators =
                allDeletions.stream().filter(d -> d.nominatedBy != null).collect(Collectors.groupingBy(deletion -> deletion.nominatedBy));
        deletionsByNominators = sortByValueCount(deletionsByNominators);
        for (Map.Entry<String, List<PageDeletion>> entry : deletionsByNominators.entrySet()) {
            final int nominatedTotal = entry.getValue().size();
            final List<PageDeletion> completed =
                    entry.getValue().stream().filter(d -> d.status == PageDeletionStatus.COMPLETED).collect(toList());
            int nominatedCompleted = completed.size();
            int nominatedPercentage = nominatedCompleted * 100 / nominatedTotal;
            int nominatedDeleted =
                    (int) completed.stream().filter(d -> d.finalSummaryStatus == PageDeletionCompletedStatus.DELETED
                            || d.finalSummaryStatus == PageDeletionCompletedStatus.DELETED_WITH_REDIRECT).count();
            int nominatedDeletedPercentage = nominatedDeleted * 100 / nominatedTotal;
            int nominatedKept =
                    (int) completed.stream().filter(d -> d.finalSummaryStatus == PageDeletionCompletedStatus.KEPT).count();
            int nominatedKeptPercentage = nominatedKept * 100 / nominatedTotal;
            report.append(String.format("| %s || %d || %d (%d%%) || %d (%d%%) || %d (%d%%) %n", entry.getKey(),
                    nominatedTotal,
                    nominatedCompleted, nominatedPercentage, nominatedDeleted, nominatedDeletedPercentage,
                    nominatedKept, nominatedKeptPercentage));
            report.append("|-\n");
        }
        report.append("|}\n");
    }

    private void displaySummarizers(List<PageDeletion> allDeletions, StringBuilder report) {
        report.append("== Адміністратори ==\n");
        report.append("{| class=\"wikitable sortable\"\n");
        report.append("|-\n");
        report.append("! Адміністратор !! Кількість підсумків !! Вилучено (%) !! Залишено (%) !! Невідомо\n");
        report.append("|-\n");
        Map<String, List<PageDeletion>> deletionsBySummarizers =
                allDeletions.stream().filter(d -> d.finalSummaryAdmin != null).collect(Collectors.groupingBy(deletion -> deletion.finalSummaryAdmin));
        deletionsBySummarizers = sortByValueCount(deletionsBySummarizers);
        for (Map.Entry<String, List<PageDeletion>> entry : deletionsBySummarizers.entrySet()) {
            String administrator = entry.getKey();
            int totalSummaries = entry.getValue().size();
            int countDeleted =
                    (int) entry.getValue().stream().filter(d -> d.finalSummaryStatus == PageDeletionCompletedStatus.DELETED || d.finalSummaryStatus == PageDeletionCompletedStatus.DELETED_WITH_REDIRECT).count();
            int countDeletedPercentage = countDeleted * 100 / totalSummaries;
            int countKept =
                    (int) entry.getValue().stream().filter(d -> d.finalSummaryStatus == PageDeletionCompletedStatus.KEPT).count();
            int countKeptPercentage = countKept * 100 / totalSummaries;
            int countOthers =
                    (int) entry.getValue().stream().filter(d -> d.finalSummaryStatus == PageDeletionCompletedStatus.UNKNOWN || d.finalSummaryStatus == PageDeletionCompletedStatus.UNNOMINATED).count();
            report.append(String.format("| %s || %d || %d (%d%%) || %d (%d%%) || %d %n", administrator, totalSummaries,
                    countDeleted, countDeletedPercentage, countKept, countKeptPercentage, countOthers));
            report.append("|-\n");
        }
        report.append("|}\n");
    }

    private <K, V> Map<K, List<V>> sortByValueCount(Map<K, List<V>> map) {
        List<Map.Entry<K, List<V>>> list = new LinkedList<>(map.entrySet());

        Collections.sort(list, (o1, o2) -> Integer.compare(o2.getValue().size(), o1.getValue().size()));

        Map<K, List<V>> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<K, List<V>> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

}
