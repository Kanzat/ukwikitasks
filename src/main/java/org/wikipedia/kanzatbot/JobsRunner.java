package org.wikipedia.kanzatbot;

import lombok.extern.slf4j.Slf4j;
import org.fastily.jwiki.core.Wiki;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.wikipedia.kanzatbot.auth.Auth;
import org.wikipedia.kanzatbot.copyvio.CopyvioDetector;
import org.wikipedia.kanzatbot.deletedrestored.DeletedPagesRestored;
import org.wikipedia.kanzatbot.potd.CreateCandidateImagesPage;
import org.wikipedia.kanzatbot.potd.ImportImages;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class JobsRunner implements CommandLineRunner {

    @Autowired
    Auth auth;
    @Autowired
    WikiFactory wikiFactory;

    @Autowired
    CreateCandidateImagesPage createCandidateImagesPage;
    @Autowired
    ImportImages importImages;
    @Autowired
    CopyvioDetector copyvioDetector;
    @Autowired
    DeletedPagesRestored deletedPagesRestored;

    @Override
    public void run(String... args) {
        final Wiki ukWiki = wikiFactory.getWikipedia("uk");
        final Wiki commonsWiki = wikiFactory.getCommons();
        this.auth.auth(ukWiki);
        List<ThrowableRunnable> jobs = new ArrayList<>();
        jobs.add(() -> createCandidateImagesPage.create(ukWiki, commonsWiki, "Вікіпедія:Вікі любить пам'ятки/Переможці", "Вікіпедія:Проєкт:Вибране зображення/Кандидати/Вікі любить пам'ятки", "Ця сторінка містить [[Вікіпедія:Вікі любить пам'ятки/Переможці|переможців конкурсу Вікі любить пам'ятки]], які ще жоден раз не ставали вибраним зображенням на головній сторінці"));
        jobs.add(() -> createCandidateImagesPage.create(ukWiki, commonsWiki, "Вікіпедія:Вікі любить Землю/Переможці", "Вікіпедія:Проєкт:Вибране зображення/Кандидати/Вікі любить Землю", "Ця сторінка містить [[Вікіпедія:Вікі любить Землю/Переможці|переможців конкурсу Вікі любить Землю]], які ще жоден раз не ставали вибраним зображенням на головній сторінці"));
        jobs.add(() -> importImages.startImport(ukWiki, commonsWiki));
        // jobs.add(() -> createCandidatePatrolsPage.create(ukWiki));
        jobs.add(() -> copyvioDetector.runInNewPages(ukWiki));
        jobs.add(() -> deletedPagesRestored.find(ukWiki));

        boolean hasFailure = false;
        for (ThrowableRunnable job : jobs) {
            if (!runIsolated(job)) {
                hasFailure = true;
            }
        }
        if (hasFailure) {
            throw new RuntimeException("There is a failed job");
        }
    }

    private boolean runIsolated(ThrowableRunnable runnable) {
        try {
            runnable.run();
            return true;
        } catch (Exception e) {
            log.error("Operation failed", e);
            return false;
        }
    }

}
