package org.wikipedia.kanzatbot.potd;

import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikipedia.kanzatbot.jwiki.JWikiUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.wikipedia.kanzatbot.Main.commonsWiki;

public class CreateCandidateImagesPage {

    private static final Logger logger = LoggerFactory.getLogger(CreateCandidateImagesPage.class);

    // Check pictures are not used as picture of the day
    public static void create(Wiki startWiki, final String startPage, Wiki endWiki, String endPage, String intro) {
        final ArrayList<String> imagesOnPage = startWiki.getImagesOnPage(startPage);
        final List<String> usedAsPod = new ArrayList<>();
        final List<String> notUsedAsPod = new ArrayList<>();
        final List<String> filteredImages = new ArrayList<>();
        for (String imageOnPage : imagesOnPage) {
            if (imageOnPage.startsWith("Файл:")) {
                imageOnPage = "File:" + imageOnPage.substring("Файл:".length());
            }
            if (isTechnicalImage(imageOnPage)) {
                continue;
            }
            filteredImages.add(imageOnPage);
        }

        final HashMap<String, ArrayList<Tuple<String, String>>> globalUsageMap = JWikiUtils.globalUsageEnhanced(commonsWiki, filteredImages, "ukwiki", "10");

        for (String filteredImage : filteredImages) {
            final ArrayList<Tuple<String, String>> tuples = globalUsageMap.get(filteredImage);
            boolean isPod = tuples.stream().anyMatch(tuple -> tuple.y.equals("uk.wikipedia.org") && tuple.x.matches(Pattern.quote("Шаблон:Potd/") + "\\d{4}-\\d{2}"));
            if (isPod) {
                usedAsPod.add(filteredImage);
            } else {
                notUsedAsPod.add(filteredImage);
            }
        }

        final String generatedMarkup = generateMarkup(intro, notUsedAsPod);
        final boolean success = endWiki.edit(endPage, generatedMarkup, "Автоматичне оновлення інформації");
        logger.info("Successful operation? " + success);
    }

    private static boolean isTechnicalImage(final String s) {
        if (s.contains("Blacked out")) {
            return true;
        }
        if (s.equals("File:Пірогово.jpg") || s.equals("File:Windmill at Pyrohiv Museum..jpg")) {
            // Обговорення Вікіпедії:Проєкт:Вибране зображення/Кандидати/Вікі любить пам'ятки
            return true;
        }
        switch (s) {
            case "File:F icon.svg":
            case "File:10 green.svg":
            case "File:10turquoise.png":
            case "File:1 green.svg":
            case "File:2 green.svg":
            case "File:3 green.svg":
            case "File:4 green.svg":
            case "File:5 green.svg":
            case "File:6 green.svg":
            case "File:7 green.svg":
            case "File:8 green.svg":
            case "File:9 green.svg":
            case "File:Cscr-featured.svg":
            case "File:Quality images logo.svg":
            case "File:WLE Austria Logo (no text).svg":
            case "File:Wiki Loves Monuments Logo notext.svg":
                return true;
        }
        return false;
    }

    private static String generateMarkup(String intro, final Collection<String> images) {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("uk","UA"));
        final String dateFormatted = LocalDate.now().format(formatter);
        StringBuilder sb = new StringBuilder();
        sb.append(intro + " (станом на " + dateFormatted + " року).\n");
        sb.append("\n");
        sb.append("<gallery mode=packed-overlay>\n");
        for (String image : images) {
            sb.append(image + "\n");
        }
        sb.append("</gallery>\n");
        sb.append("\n");
        sb.append("[[Категорія:Вікіпедія:Проєкт:Вибране зображення]]\n");
        return sb.toString();
    }

}
