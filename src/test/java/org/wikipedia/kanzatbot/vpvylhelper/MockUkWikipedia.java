package org.wikipedia.kanzatbot.vpvylhelper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.fastily.jwiki.core.Wiki;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * A real jwiki {@link Wiki} connected to a local MockWebServer that answers with responses recorded from
 * uk.wikipedia.org on 2026-10-31 (see README.md next to the JSON files). Requests are matched by their parameters, not
 * by their order. A failing request gets HTTP 500, which jwiki handles exactly as it handles a real outage.
 */
class MockUkWikipedia extends ExternalResource {

    /** The only page that exists on this wiki: 10 nominations, 7 of them with a summary. */
    static final String DAY_PAGE = "Вікіпедія:Статті-кандидати на вилучення/15 вересня 2026";

    private final MockWebServer server = new MockWebServer();
    private final Map<String, Integer> loadFailures = new HashMap<>();
    private final Map<String, Integer> existsFailures = new HashMap<>();
    private final List<String> loadRequests = Collections.synchronizedList(new ArrayList<>());
    private final List<String> existsRequests = Collections.synchronizedList(new ArrayList<>());
    private final List<String> unexpectedRequests = Collections.synchronizedList(new ArrayList<>());
    private Wiki wiki;

    @Override
    protected void before() throws IOException {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return respond(request);
            }
        });
        server.start();
        wiki = new Wiki.Builder().withApiEndpoint(server.url("/w/api.php")).withDefaultLogger(false).build();
    }

    @Override
    protected void after() {
        try {
            assertEquals("requests the mock doesn't know how to answer", List.of(), unexpectedRequests);
        } finally {
            try {
                server.shutdown();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    Wiki wiki() {
        return wiki;
    }

    /** The next {@code times} attempts to load {@code title} fail. */
    MockUkWikipedia failLoads(String title, int times) {
        loadFailures.put(title, times);
        return this;
    }

    /** The next {@code times} existence checks of {@code title} fail. */
    MockUkWikipedia failExistenceChecks(String title, int times) {
        existsFailures.put(title, times);
        return this;
    }

    /** Titles of the pages jwiki tried to load, in order. */
    List<String> loadRequests() {
        return List.copyOf(loadRequests);
    }

    /** Titles jwiki checked for existence, in order. */
    List<String> existsRequests() {
        return List.copyOf(existsRequests);
    }

    private MockResponse respond(RecordedRequest request) {
        String action = request.getRequestUrl().queryParameter("action");
        String prop = request.getRequestUrl().queryParameter("prop");
        String meta = request.getRequestUrl().queryParameter("meta");
        if ("query".equals(action) && "siteinfo".equals(meta)) {
            return json(resource("siteinfo.json"));
        }
        if ("parse".equals(action) && "sections".equals(prop)) {
            String title = request.getRequestUrl().queryParameter("page");
            loadRequests.add(title);
            if (consumeFailure(loadFailures, title)) {
                return new MockResponse().setResponseCode(500);
            }
            return json(resource(DAY_PAGE.equals(title) ? "day-page-sections.json" : "missing-page-parse.json"));
        }
        if ("query".equals(action) && "revisions".equals(prop)) {
            String title = request.getRequestUrl().queryParameter("titles");
            if (DAY_PAGE.equals(title)) {
                return json(resource("day-page-text.json"));
            }
        }
        if ("query".equals(action) && "pageprops".equals(prop)) {
            String title = request.getRequestUrl().queryParameter("titles");
            existsRequests.add(title);
            if (consumeFailure(existsFailures, title)) {
                return new MockResponse().setResponseCode(500);
            }
            return json(existsResponse(title));
        }
        unexpectedRequests.add(request.getMethod() + " " + request.getRequestUrl());
        return new MockResponse().setResponseCode(404);
    }

    private static boolean consumeFailure(Map<String, Integer> failures, String title) {
        int left = failures.getOrDefault(title, 0);
        if (left == 0) {
            return false;
        }
        failures.put(title, left - 1);
        return true;
    }

    /**
     * The recorded answer for an existing or a missing page, with the title swapped in: jwiki looks the answer up by
     * the title it asked for.
     */
    private static String existsResponse(String title) {
        JsonObject response = JsonParser.parseString(
                resource(DAY_PAGE.equals(title) ? "existing-page.json" : "missing-page.json")).getAsJsonObject();
        JsonObject pages = response.getAsJsonObject("query").getAsJsonObject("pages");
        String pageId = pages.keySet().iterator().next();
        pages.getAsJsonObject(pageId).addProperty("title", title);
        return response.toString();
    }

    private static MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json; charset=utf-8").setBody(body);
    }

    private static String resource(String name) {
        try (InputStream in = MockUkWikipedia.class.getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalStateException("Missing test resource " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
