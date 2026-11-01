package org.wikipedia.kanzatbot.copyviopages;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.dwrap.Revision;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CopyvioPagesTest {

    private static final String PAGE = "Стаття";
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

    private final Wiki wiki = mock(Wiki.class);

    @Before
    public void setUp() {
        // Revisions without the template in their text are also checked with the parser; it finds no templates either
        when(wiki.basicPOST(eq("parse"), any())).thenAnswer(invocation -> parseTree("<root>текст</root>"));
    }

    @Test
    public void findsCopyvioTemplates() {
        assertTrue(CopyvioPages.containsCopyvioTemplate("Текст\n{{Copyvio|url=https://example.com}}"));
        assertTrue(CopyvioPages.containsCopyvioTemplate("{{COPYVIO}}"));
        assertTrue(CopyvioPages.containsCopyvioTemplate("{{копіво}}"));
        assertTrue(CopyvioPages.containsCopyvioTemplate("{{Порушення_авторських_прав|дата=1 листопада 2026}}"));
        assertTrue(CopyvioPages.containsCopyvioTemplate("{{Дослівний переклад|en|Article}}"));
    }

    @Test
    public void ignoresTextWithoutCopyvioTemplates() {
        assertFalse(CopyvioPages.containsCopyvioTemplate(""));
        assertFalse(CopyvioPages.containsCopyvioTemplate("{{Картка:Особа}} Стаття про copyvio"));
    }

    @Test
    public void findsTheRevisionThatAddedTheTemplate() {
        Revision added = revision("2026-10-15", "{{Copyvio}} текст");
        givenHistory(List.of(
                revision("2026-10-20", "{{Copyvio}} текст, правка"),
                revision("2026-10-18", "{{Copyvio}} текст"),
                added,
                revision("2026-10-10", "текст"),
                revision("2026-10-05", "чернетка")));

        assertSame(added, CopyvioPages.findRevisionThatAddedTemplate(wiki, PAGE));
        verify(wiki, never()).getRevisions(PAGE, 0, false, null, null);
    }

    @Test
    public void looksThroughTheWholeHistoryWhenTheLastRevisionsAllHaveTheTemplate() {
        List<Revision> revisions = new ArrayList<>();
        for (int day = 30; day >= 18; day--) {
            revisions.add(revision("2026-10-" + day, "{{Copyvio}} текст, правка " + day));
        }
        Revision added = revisions.get(revisions.size() - 1);
        revisions.add(revision("2026-10-10", "текст"));
        givenHistory(revisions);

        assertSame(added, CopyvioPages.findRevisionThatAddedTemplate(wiki, PAGE));
        verify(wiki).getRevisions(PAGE, 0, false, null, null);
    }

    @Test
    public void countsOnlyTheLastRunOfRevisionsWithTheTemplate() {
        Revision readded = revision("2026-10-18", "{{Copyvio}} текст");
        givenHistory(List.of(
                revision("2026-10-20", "{{Copyvio}} текст, правка"),
                readded,
                revision("2026-10-15", "текст"),
                revision("2026-10-10", "{{Copyvio}} текст")));

        assertSame(readded, CopyvioPages.findRevisionThatAddedTemplate(wiki, PAGE));
    }

    @Test
    public void findsNothingWhenTheNewestRevisionHasNoTemplate() {
        givenHistory(List.of(
                revision("2026-10-20", "текст"),
                revision("2026-10-15", "{{Copyvio}} текст")));

        assertNull(CopyvioPages.findRevisionThatAddedTemplate(wiki, PAGE));
        verify(wiki, never()).getRevisions(PAGE, 0, false, null, null);
    }

    @Test
    public void findsTemplatesThatOnlyTheParserRecognizes() {
        Revision added = revision("2026-10-20", "{{ Copyvio }} текст");
        assertFalse(CopyvioPages.containsCopyvioTemplate(added.text));
        when(wiki.basicPOST(eq("parse"), argThat(form -> added.text.equals(form.get("text")))))
                .thenAnswer(invocation -> parseTree("<root><template><title> Copyvio </title></template> текст</root>"));
        givenHistory(List.of(added, revision("2026-10-15", "текст")));

        assertSame(added, CopyvioPages.findRevisionThatAddedTemplate(wiki, PAGE));
    }

    @Test
    public void rendersPagesNewestDateFirst() {
        Map<LocalDate, List<String>> pagesByDate = Map.of(
                LocalDate.of(2026, 10, 28), List.of("Варта"),
                LocalDate.of(2026, 10, 30), List.of("Бобер", "Абрикос"));

        assertEquals("{{/шапка}}\n\n"
                        + "{{Вирішення проблем авторського права|2026-10-30|[[Абрикос]] • [[Бобер]]}}\n\n"
                        + "{{Вирішення проблем авторського права|2026-10-28|[[Варта]]}}\n\n"
                        + "{{/низ}}\n\n",
                CopyvioPages.renderPage(pagesByDate, TODAY));
    }

    @Test
    public void putsTheMessageBeforeTheFirstDateOlderThanAWeek() {
        Map<LocalDate, List<String>> pagesByDate = Map.of(
                LocalDate.of(2026, 10, 25), List.of("Рівно тиждень"),
                LocalDate.of(2026, 10, 24), List.of("Вісім днів"),
                LocalDate.of(2026, 2, 9), List.of("Давня"));

        assertEquals("{{/шапка}}\n\n"
                        + "{{Вирішення проблем авторського права|2026-10-25|[[Рівно тиждень]]}}\n\n"
                        + "{{Вирішення проблем авторського права/Message}}\n\n"
                        + "{{Вирішення проблем авторського права|2026-10-24|[[Вісім днів]]}}\n\n"
                        + "{{Вирішення проблем авторського права|2026-2-9|[[Давня]]}}\n\n"
                        + "{{/низ}}\n\n",
                CopyvioPages.renderPage(pagesByDate, TODAY));
    }

    @Test
    public void rendersAnEmptyCategory() {
        assertEquals("{{/шапка}}\n\n{{/низ}}\n\n", CopyvioPages.renderPage(Map.of(), TODAY));
    }

    /** The page history, newest first, as {@code Wiki.getRevisions} returns it: the last 10 revisions or all. */
    private void givenHistory(List<Revision> newestFirst) {
        when(wiki.getRevisions(PAGE, 10, false, null, null))
                .thenReturn(new ArrayList<>(newestFirst.subList(0, Math.min(10, newestFirst.size()))));
        when(wiki.getRevisions(PAGE, 0, false, null, null)).thenReturn(new ArrayList<>(newestFirst));
    }

    private static Revision revision(String date, String text) {
        Revision revision = new Revision() {
        };
        revision.timestamp = Instant.parse(date + "T12:00:00Z");
        revision.text = text;
        return revision;
    }

    /** The answer of {@code action=parse&prop=parsetree} with the given parse tree. */
    private static Response parseTree(String xml) {
        return new Response.Builder()
                .request(new Request.Builder().url("https://uk.wikipedia.org/w/api.php").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(ResponseBody.create("{\"parse\":{\"parsetree\":{\"*\":\"" + xml + "\"}}}",
                        MediaType.get("application/json")))
                .build();
    }
}
