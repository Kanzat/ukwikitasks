package org.wikipedia.kanzatbot.vpvylhelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.wikipedia.kanzatbot.vpvylhelper.MockUkWikipedia.DAY_PAGE;

public class VpVylParserTest {

    private static final String MISSING_DAY_PAGE = "Вікіпедія:Статті-кандидати на вилучення/14 грудня 2025";

    @Rule
    public final MockUkWikipedia uk = new MockUkWikipedia();

    private final VpVylParser parser = new VpVylParser();
    private final List<Long> sleeps = new ArrayList<>();

    @Before
    public void setUp() {
        parser.sleeper = sleeps::add;
    }

    @After
    public void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    public void parsesDayPage() {
        List<PageDeletion> deletions = parser.getDeletions(uk.wiki(), DAY_PAGE);

        // The recorded revision: https://uk.wikipedia.org/w/index.php?oldid=48886016
        // Columns: title | nominator | for | against | status | result | admin who wrote the summary
        assertEquals(Arrays.asList(
                "Файл:Mitiukov.png | Бананчик | [Бананчик, Шабля] | [] | COMPLETED | DELETED | Duppertip",
                "Стелла (Winx Club) | Шабля | [Шабля, Duppertip] | [] | COMPLETED | DELETED | Mr.Rosewater",
                "Флора (Winx Club) | Шабля | [Шабля, Duppertip] | [] | COMPLETED | DELETED | Mr.Rosewater",
                "Хмельницький інститут соціальних технологій Університету «Україна» | J. Gradowski"
                        + " | [J. Gradowski, Анатолій 2000] | [Volodymyr D-k] | IN_PROGRESS | null | null",
                "Лашкул Георгій Олексійович | Шабля | [Шабля, Анатолій 2000] | [Duppertip] | IN_PROGRESS | null | null",
                "Гребенєв Сергій Миколайович | Friend | [Friend] | [] | COMPLETED | DELETED | Duppertip",
                "Дудник Сергій Павлович | Friend | [Friend] | [] | COMPLETED | DELETED | Duppertip",
                "Караков Олександр Садійович | Friend | [Friend] | [] | COMPLETED | DELETED | Mr.Rosewater",
                // Wrong: Goo3 deleted the page, BotDR only wrote the summary. See TODO.md
                "Kono Media Team | Динамо-фан | [Динамо-фан] | [] | COMPLETED | DELETED | BotDR",
                "Армат (медіафутбольний клуб) | Динамо-фан | [Динамо-фан] | [] | IN_PROGRESS | null | null"),
                deletions.stream().map(VpVylParserTest::row).collect(toList()));
        assertTrue(deletions.stream().allMatch(d -> DAY_PAGE.equals(d.location)));

        assertEquals(Collections.singletonList(DAY_PAGE), uk.loadRequests());
        assertTrue("existence isn't checked when the page loaded", uk.existsRequests().isEmpty());
    }

    @Test
    public void missingPageGivesNoDeletionsWithoutRetrying() {
        assertTrue(parser.getDeletions(uk.wiki(), MISSING_DAY_PAGE).isEmpty());

        assertEquals(1, uk.loadRequests().size());
        assertEquals(Collections.singletonList(MISSING_DAY_PAGE), uk.existsRequests());
        assertTrue(sleeps.isEmpty());
    }

    @Test
    public void retriesWhenExistingPageFailedToLoad() {
        // The 2026-10-28 incident: the page exists, but the request failed and jwiki returned no sections.
        uk.failLoads(DAY_PAGE, 2);

        List<PageDeletion> deletions = parser.getDeletions(uk.wiki(), DAY_PAGE);

        assertEquals(10, deletions.size());
        assertEquals(3, uk.loadRequests().size());
        assertEquals(Arrays.asList(VpVylParser.RETRY_DELAYS_MS[0], VpVylParser.RETRY_DELAYS_MS[1]), sleeps);
    }

    @Test
    public void retriesWhenExistenceCheckAlsoFails() {
        uk.failLoads(DAY_PAGE, 1).failExistenceChecks(DAY_PAGE, 1);

        List<PageDeletion> deletions = parser.getDeletions(uk.wiki(), DAY_PAGE);

        assertEquals(10, deletions.size());
        assertEquals(Collections.singletonList(VpVylParser.RETRY_DELAYS_MS[0]), sleeps);
    }

    @Test
    public void pageConfirmedMissingOnRetryGivesNoDeletions() {
        uk.failExistenceChecks(MISSING_DAY_PAGE, 1);

        assertTrue(parser.getDeletions(uk.wiki(), MISSING_DAY_PAGE).isEmpty());
        assertEquals(2, uk.loadRequests().size());
    }

    @Test
    public void failsInsteadOfReturningPartialDataWhenRetriesAreExhausted() {
        uk.failLoads(DAY_PAGE, Integer.MAX_VALUE);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> parser.getDeletions(uk.wiki(), DAY_PAGE));

        assertTrue(e.getMessage(), e.getMessage().contains(DAY_PAGE));
        assertEquals(VpVylParser.RETRY_DELAYS_MS.length + 1, uk.loadRequests().size());
        assertEquals(VpVylParser.RETRY_DELAYS_MS.length, sleeps.size());
        for (int i = 0; i < sleeps.size(); i++) {
            assertEquals(VpVylParser.RETRY_DELAYS_MS[i], (long) sleeps.get(i));
        }
    }

    @Test
    public void interruptionStopsRetryingAndKeepsInterruptFlag() {
        parser.sleeper = millis -> {
            throw new InterruptedException();
        };
        uk.failLoads(DAY_PAGE, 1);

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> parser.getDeletions(uk.wiki(), DAY_PAGE));

        assertTrue(e.getCause() instanceof InterruptedException);
        assertTrue(Thread.currentThread().isInterrupted());
        assertEquals(1, uk.loadRequests().size());
    }

    private static String row(PageDeletion d) {
        return String.join(" | ", d.title, d.nominatedBy, String.valueOf(d.votedApprove), String.valueOf(d.votedReject),
                String.valueOf(d.status), String.valueOf(d.finalSummaryStatus), String.valueOf(d.finalSummaryAdmin));
    }
}
