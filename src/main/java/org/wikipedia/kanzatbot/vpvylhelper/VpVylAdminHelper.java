package org.wikipedia.kanzatbot.vpvylhelper;

import lombok.extern.slf4j.Slf4j;
import org.fastily.jwiki.core.NS;
import org.fastily.jwiki.core.Wiki;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static java.util.stream.Collectors.toList;

@Slf4j
@Component
public class VpVylAdminHelper {

    @Autowired
    VpVylParser vpVylParser;

    public void generateReport(final Wiki wiki) {
        List<String> categoryMembers = wiki.getCategoryMembers("Категорія:Незавершені обговорення вилучення сторінок"
                , NS.PROJECT);
        List<String> filteredPages = categoryMembers.stream().filter(cm -> cm.startsWith("Вікіпедія:Статті-кандидати " +
                "на вилучення/")).collect(toList());
        filteredPages.remove("Вікіпедія:Статті-кандидати на вилучення/Заготовка");

        LocalDate endDate = LocalDate.now().minus(7, ChronoUnit.DAYS);

        List<PageDeletion> deletionsInProgress = new ArrayList<>();
        for (String filteredPage : filteredPages) {
            List<PageDeletion> deletions = vpVylParser.getDeletions(wiki, filteredPage);
            deletions.stream().filter(d -> d.status != PageDeletionStatus.COMPLETED).filter(d -> getDateFromTitle(d).isBefore(endDate)).forEach(deletionsInProgress::add);
        }

        deletionsInProgress.sort((c1, c2) -> getDateFromTitle(c2).compareTo(getDateFromTitle(c1)));

        StringBuilder sb = new StringBuilder();
        sb.append("== Неспірні номінації ==\n");
        List<PageDeletion> simpleDeletions = deletionsInProgress.stream().filter(pd -> pd.votedAgainst == 0 || pd.votedFor == 0).collect(toList());
        display(sb, simpleDeletions);
        deletionsInProgress.removeAll(simpleDeletions);
        sb.append("== Спірні номінації ==\n");
        List<PageDeletion> mediumDeletions =
                deletionsInProgress.stream().filter(pd -> pd.status != PageDeletionStatus.CONTESTED).collect(toList());
        display(sb, mediumDeletions);
        deletionsInProgress.removeAll(mediumDeletions);
        sb.append("== Оскарження ==\n");
        display(sb, deletionsInProgress);

        wiki.edit("User:KanzatBot/Reports/DeletionHelper", sb.toString(), "Оновлення звіту по ВП:ВИЛ");
    }

    private void display(StringBuilder report, List<PageDeletion> deletions) {
        report.append("{| class=\"wikitable sortable\"\n");
        report.append("|-\n");
        report.append("! # !! Назва !! Посилання !! За !! Проти \n");
        report.append("|-\n");
        for (int i = 0; i < deletions.size(); i++) {
            PageDeletion deletion = deletions.get(i);
            report.append("| " + (i + 1) + " || " + getTitleLink(deletion) + " || " + getFullLocation(deletion) +
                    " || " + deletion.votedFor + " " +
                    "|| " + deletion.votedAgainst +
                    "\n");
            report.append("|-\n");
        }
        report.append("|}\n");
    }

    private String getTitleLink(PageDeletion deletion) {
        if (deletion.title.contains(":")) {
            return "[[:" + deletion.title + "]]";
        }
        return "[[" + deletion.title + "]]";
    }

    private String getFullLocation(PageDeletion deletion) {
        return "[[" + deletion.location + "#" + deletion.title + "|" + deletion.location + "]]";
    }

    private LocalDate getDateFromTitle(PageDeletion deletion) {
        if (!deletion.location.startsWith("Вікіпедія:Статті-кандидати на вилучення/")) {
            return LocalDate.MIN;
        }
        try {
            String sublocation = deletion.location.substring("Вікіпедія:Статті-кандидати на вилучення/".length());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("uk", "UA"));
            return LocalDate.parse(sublocation, formatter);
        } catch (DateTimeParseException e) {
            return LocalDate.MIN;
        }
    }


}
