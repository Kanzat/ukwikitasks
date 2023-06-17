package org.wikipedia.kanzatbot;

import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.fastily.jwiki.core.Wiki;
import org.wikipedia.kanzatbot.auth.Auth;
import org.wikipedia.kanzatbot.copyvio.CopyvioDetector;
import org.wikipedia.kanzatbot.deletedrestored.DeletedPagesRestored;
import org.wikipedia.kanzatbot.potd.CreateCandidateImagesPage;
import org.wikipedia.kanzatbot.potd.ImportImages;

@Slf4j
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
        runIsolated(() -> CreateCandidateImagesPage.create(ukWiki, "Вікіпедія:Вікі любить пам'ятки/Переможці", ukWiki, "Вікіпедія:Проєкт:Вибране зображення/Кандидати/Вікі любить пам'ятки",
                "Ця сторінка містить [[Вікіпедія:Вікі любить пам'ятки/Переможці|переможців конкурсу Вікі любить пам'ятки]], які ще жоден раз не ставали вибраним зображенням на головній сторінці"));
        runIsolated(() -> CreateCandidateImagesPage.create(ukWiki, "Вікіпедія:Вікі любить Землю/Переможці", ukWiki, "Вікіпедія:Проєкт:Вибране зображення/Кандидати/Вікі любить Землю",
                "Ця сторінка містить [[Вікіпедія:Вікі любить Землю/Переможці|переможців конкурсу Вікі любить Землю]], які ще жоден раз не ставали вибраним зображенням на головній сторінці"));
        runIsolated(() -> ImportImages.startImport());
        // runIsolated(() -> CreateCandidatePatrolsPage.create());
        runIsolated(() -> CopyvioDetector.runInNewPages(ukWiki));
        runIsolated(() -> DeletedPagesRestored.find(ukWiki));
    }

    private static void runIsolated(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.error("Operation failed", e);
        }
    }


}
