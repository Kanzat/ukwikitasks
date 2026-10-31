package org.wikipedia.kanzatbot.vpvylhelper;

import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.wikipedia.kanzatbot.vpvylhelper.MockUkWikipedia.DAY_PAGE;

public class VpVylParserTest {

    private static final String MISSING_DAY_PAGE = "Вікіпедія:Статті-кандидати на вилучення/14 грудня 2025";

    @Rule
    public final MockUkWikipedia uk = new MockUkWikipedia();

    private final VpVylParser parser = new VpVylParser();

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
    }

    @Test
    public void missingPageGivesNoDeletions() {
        assertTrue(parser.getDeletions(uk.wiki(), MISSING_DAY_PAGE).isEmpty());
    }

    private static String row(PageDeletion d) {
        return String.join(" | ", d.title, d.nominatedBy, String.valueOf(d.votedApprove), String.valueOf(d.votedReject),
                String.valueOf(d.status), String.valueOf(d.finalSummaryStatus), String.valueOf(d.finalSummaryAdmin));
    }
}
