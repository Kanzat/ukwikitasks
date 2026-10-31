package org.wikipedia.kanzatbot.vpvylhelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VpVylStatsTest {

    /** The 365 days before it include {@link MockUkWikipedia#DAY_PAGE}; every other day page is missing. */
    private static final LocalDate END = LocalDate.of(2026, 10, 29);
    private static final String PREFIX = "Вікіпедія:Статті-кандидати на вилучення/";

    @Rule
    public final MockUkWikipedia uk = new MockUkWikipedia();

    private final VpVylStats stats = new VpVylStats();

    @Before
    public void setUp() {
        stats.vpVylParser = new VpVylParser();
    }

    @Test
    public void readsEveryDayOfThePastYear() {
        stats.buildReport(uk.wiki(), END);

        List<String> titles = uk.loadRequests();
        assertEquals(365, titles.size());
        assertEquals(PREFIX + "29 жовтня 2025", titles.get(0));
        assertEquals(PREFIX + "28 жовтня 2026", titles.get(364));
    }

    @Test
    public void countsNominationsAndSummaries() {
        String report = stats.buildReport(uk.wiki(), END);

        assertTrue(report, report.contains("| Шабля || 3 || 2 (66%) || 2 (66%) || 0 (0%) "));
        assertTrue(report, report.contains("| Friend || 3 || 3 (100%) || 3 (100%) || 0 (0%) "));
        assertTrue(report, report.contains("| Динамо-фан || 2 || 1 (50%) || 1 (50%) || 0 (0%) "));
        assertTrue(report, report.contains("| J. Gradowski || 1 || 0 (0%) || 0 (0%) || 0 (0%) "));
        assertTrue(report, report.contains("| Duppertip || 3 || 3 (100%) || 0 (0%) || 0 "));
        assertTrue(report, report.contains("| Mr.Rosewater || 3 || 3 (100%) || 0 (0%) || 0 "));
        assertTrue("nominators are sorted by count",
                report.indexOf("| Friend ||") < report.indexOf("| Динамо-фан ||"));
    }
}
