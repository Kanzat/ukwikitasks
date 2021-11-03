package org.wikipedia.kanzatbot;

import okhttp3.HttpUrl;
import org.fastily.jwiki.core.Wiki;
import org.wikipedia.kanzatbot.auth.Auth;
import org.wikipedia.kanzatbot.potd.CreateCandidateImagesPage;
import org.wikipedia.kanzatbot.potd.ImportImages;

public class Main {

    public static Wiki ukWiki;
    public static Wiki commonsWiki;

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected 1 argument (path to credentials file)");
        }
        Auth.credentialsFile = args[0];
        ukWiki = new Wiki.Builder().withApiEndpoint(HttpUrl.parse("https://uk.wikipedia.org/w/api.php")).build();
        commonsWiki = new Wiki.Builder().withApiEndpoint(HttpUrl.parse("https://commons.wikimedia.org/w/api.php")).build();
        Auth.auth(ukWiki);
        CreateCandidateImagesPage.create(ukWiki, "Вікіпедія:Вікі любить пам'ятки/Переможці", ukWiki, "Вікіпедія:Проєкт:Вибране зображення/Кандидати/Вікі любить пам'ятки",
                "Ця сторінка містить [[Вікіпедія:Вікі любить пам'ятки/Переможці|переможців конкурсу Вікі любить пам'ятки]], які ще жоден раз не ставали вибраним зображенням на головній сторінці");
        CreateCandidateImagesPage.create(ukWiki, "Вікіпедія:Вікі любить Землю/Переможці", ukWiki, "Вікіпедія:Проєкт:Вибране зображення/Кандидати/Вікі любить Землю",
                "Ця сторінка містить [[Вікіпедія:Вікі любить Землю/Переможці|переможців конкурсу Вікі любить Землю]], які ще жоден раз не ставали вибраним зображенням на головній сторінці");
        ImportImages.startImport();
        // CreateCandidatePatrolsPage.create();
    }

}
