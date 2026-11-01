package org.wikipedia.kanzatbot.photorequests;

import lombok.extern.slf4j.Slf4j;
import org.fastily.jwiki.core.NS;
import org.fastily.jwiki.core.WParser;
import org.fastily.jwiki.core.Wiki;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Lists the users who can take a photo for Wikipedia, with the places from their {{Можу сфотографувати}}.
 */
@Slf4j
@Component
public class PhotoRequests {

    private static final String CATEGORY = "Категорія:Користувачі, що можуть зробити світлину для Вікіпедії";
    private static final String TEMPLATE = "Можу сфотографувати";
    private static final String REPORT_PAGE = "User:KanzatBot/Reports/PhotoVolunteersMap";

    public void update(Wiki ukWiki) {
        List<String> userPages = ukWiki.getCategoryMembers(CATEGORY, NS.USER);
        SortedMap<String, String> userToLocation = new TreeMap<>();
        SortedSet<String> noLocationUsers = new TreeSet<>();
        for (String userPage : userPages) {
            // Without the namespace; for a subpage also keeps the subpage name, e.g. "User/Юзербокси"
            String user = ukWiki.nss(userPage);
            WParser.WikiText wikiText = WParser.parseText(ukWiki, ukWiki.getPageText(userPage));
            if (wikiText == null) {
                log.error("Failed to parse {}", userPage);
                continue;
            }
            Optional<WParser.WTemplate> template = wikiText.getTemplatesR().stream()
                    .filter(t -> isCanPhotographTemplate(t.title))
                    .findFirst();
            if (template.isEmpty()) {
                // E.g. the template is transcluded from another page
                log.error("Failed to find {{{}}} on {}", TEMPLATE, userPage);
                continue;
            }
            String location = template.get().has("1") ? template.get().get("1").toString() : "";
            if (location.isEmpty()) {
                log.warn("User {} didn't specify location", user);
                noLocationUsers.add(user);
            } else {
                userToLocation.put(user, location);
            }
        }

        boolean success = ukWiki.edit(REPORT_PAGE, renderPage(userToLocation, noLocationUsers), "Оновлення інформації");
        log.info("Successful operation? " + success);
    }

    static boolean isCanPhotographTemplate(String title) {
        // Also matches "Шаблон:Можу сфотографувати"
        String name = title.substring(title.indexOf(':') + 1).replace('_', ' ').strip();
        // Like MediaWiki on uk.wikipedia, only the first letter is case-insensitive
        return !name.isEmpty() && Character.toUpperCase(name.charAt(0)) == TEMPLATE.charAt(0)
                && name.substring(1).equals(TEMPLATE.substring(1));
    }

    static String renderPage(SortedMap<String, String> userToLocation, SortedSet<String> noLocationUsers) {
        StringBuilder sb = new StringBuilder();
        sb.append("Перелік користувачів, які можуть зробити фотографію, відповідно до шаблону {{tl|").append(TEMPLATE)
                .append("}}. Ця сторінка допоміжна для виконання запитів на зображення (див. [[Вікіпедія:Запити на зображення]]).\n\n");
        sb.append("{| class=\"wikitable sortable\"\n");
        sb.append("|+\n");
        sb.append("! Користувач(ка)\n");
        sb.append("! Де може сфотографувати\n");
        userToLocation.forEach((user, location) -> {
            sb.append("|-\n");
            sb.append("|").append(formatUser(user)).append("\n");
            sb.append("|").append(location).append("\n");
        });
        sb.append("|}\n");
        if (!noLocationUsers.isEmpty()) {
            sb.append("\n");
            sb.append("Користувачі, які поставили шаблон, але не зазначили місцевість: ")
                    .append(noLocationUsers.stream().map(PhotoRequests::formatUser).collect(Collectors.joining(", ")));
        }
        return sb.toString();
    }

    private static String formatUser(String user) {
        return "[[User:" + user + "|" + user + "]]";
    }

}
