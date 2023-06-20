package org.wikipedia.kanzatbot.vpvylhelper;

import lombok.extern.slf4j.Slf4j;
import org.fastily.jwiki.core.Wiki;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

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
        report.append("! Номінатор !! Кількість номінацій !! З підсумком (к-сть) !! З підсумком (%) \n");
        report.append("|-\n");
        Map<String, List<PageDeletion>> deletionsByNominators =
                allDeletions.stream().filter(d -> d.nominatedBy != null).collect(Collectors.groupingBy(deletion -> deletion.nominatedBy));
        for (Map.Entry<String, List<PageDeletion>> entry : deletionsByNominators.entrySet()) {
            long completed = entry.getValue().stream().filter(deletion -> deletion.status == PageDeletionStatus.COMPLETED).count();
            int total = entry.getValue().size();
            int percentage = (int) (completed * 100 / total);
            report.append("| " + entry.getKey() + " || " + total + " || " + completed + " || " + percentage + "%\n");
            report.append("|-\n");
        }
        report.append("|}\n");
    }

    private void displaySummarizers(List<PageDeletion> allDeletions, StringBuilder report) {
        report.append("== Адміністратори ==\n");
        report.append("{| class=\"wikitable sortable\"\n");
        report.append("|-\n");
        report.append("! Адміністратор !! Кількість підсумків\n");
        report.append("|-\n");
        Map<String, List<PageDeletion>> deletionsBySummarizers =
                allDeletions.stream().filter(d -> d.finalSummaryAdmin != null).collect(Collectors.groupingBy(deletion -> deletion.finalSummaryAdmin));
        for (Map.Entry<String, List<PageDeletion>> entry : deletionsBySummarizers.entrySet()) {
            report.append("| " + entry.getKey() + " || " + entry.getValue().size() + "\n");
            report.append("|-\n");
        }
        report.append("|}\n");
    }

}
