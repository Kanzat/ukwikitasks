package org.wikipedia.kanzatbot.potd;

import org.fastily.jwiki.core.WParser;
import org.fastily.jwiki.core.Wiki;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.wikipedia.kanzatbot.Main.commonsWiki;
import static org.wikipedia.kanzatbot.Main.ukWiki;

public class ImportImages {

    private static final Logger logger = LoggerFactory.getLogger(ImportImages.class);

    public static void startImport() {
        boolean useNow = true;
        final LocalDate currentDate = useNow ? LocalDate.now() : LocalDate.of(2021, 1, 1);
        final LocalDate startDate = currentDate.plus(7, ChronoUnit.DAYS);
        final LocalDate endDate = startDate.plus(75, ChronoUnit.DAYS);
        logger.info("Start date = {}; end date = {}", startDate, endDate);
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            final String year = "" + date.getYear();
            final String month = date.getMonthValue() < 10 ? "0" + date.getMonthValue() : "" + date.getMonthValue();
            final String ukWikiDay = "" + date.getDayOfMonth();
            final String commonsDay = date.getDayOfMonth() < 10 ? "0" + date.getDayOfMonth() : "" + date.getDayOfMonth();
            final String commonsFilenameTemplate = "Template:Potd/" + date.getYear() + "-" + month + "-" + commonsDay;
            final String ukWikiFilenameTemplate = "Шаблон:Potd/" + date.getYear() + "-" + month + "-" + ukWikiDay;
            final String ukWikiDescriptionTemplate = "Шаблон:Potd/" + date.getYear() + "-" + month + "-" + ukWikiDay + " (uk)";
            if (!ukWiki.exists(ukWikiFilenameTemplate) && commonsWiki.exists(commonsFilenameTemplate)) {
                final String commonsFilenameTemplateText = commonsWiki.getPageText(commonsFilenameTemplate);
                final Optional<WParser.WTemplate> potdFilename = WParser.parseText(commonsWiki, commonsFilenameTemplateText).getTemplates().stream().filter(t -> t.title.equals("Potd filename")).findFirst();
                if (potdFilename.isPresent()) {
                    final String commonsUkDescriptionTemplate = commonsFilenameTemplate + " (uk)";
                    final String commonsEnDescriptionTemplate = commonsFilenameTemplate + " (en)";
                    Optional<String> descriptionText = Optional.empty();
                    if (commonsWiki.exists(commonsUkDescriptionTemplate)) {
                        descriptionText = parseDescriptionText(commonsWiki, commonsWiki.getPageText(commonsUkDescriptionTemplate));
                    } else if (commonsWiki.exists(commonsEnDescriptionTemplate)) {
                        descriptionText = parseDescriptionText(commonsWiki, commonsWiki.getPageText(commonsEnDescriptionTemplate));
                    }
                    createFilenamePage(ukWiki, ukWikiFilenameTemplate, potdFilename.get().get("1").toString(), year, month, ukWikiDay);
                    descriptionText.ifPresent(s -> createDescriptionPage(ukWiki, ukWikiDescriptionTemplate, s));
                }
            }
        }
    }

    private static Optional<String> parseDescriptionText(Wiki wiki, final String pageText) {
        final WParser.WikiText wikiText = WParser.parseText(wiki, pageText);
        return wikiText.getTemplates().stream().filter(t -> t.title.equals("Potd description")).findFirst().map(wTemplate -> wTemplate.get("1").toString());
    }

    private static void createFilenamePage(Wiki ukWiki, String ukWikiTitle, String fileName, String year, String month, String ukWikiDay) {
        ukWiki.edit(ukWikiTitle, "{{Potd filename|" + fileName + "|" + year + "|" + month + "|" + ukWikiDay + "}}", "Імпорт зображення дня з Вікісховища");
    }

    private static void createDescriptionPage(Wiki ukWiki, String ukWikiTitle, String description) {
        ukWiki.edit(ukWikiTitle, "<!--" + description + "--><noinclude>\n[[Категорія:Шаблони:Підписи зображенням дня]]</noinclude>", "Імпорт опису з Вікісховища");
    }

}
