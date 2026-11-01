package org.wikipedia.kanzatbot;

import okhttp3.HttpUrl;
import org.fastily.jwiki.core.Wiki;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class WikiFactory {

    private final ConcurrentMap<String, Wiki> wikipediaCache = new ConcurrentHashMap<>();
    private final String userAgent;
    private final Wiki commons;

    public WikiFactory(@Value("${BOT_USER}") String botUser) {
        this.userAgent = userAgent(botUser);
        this.commons = new Wiki.Builder().withApiEndpoint(HttpUrl.parse("https://commons.wikimedia.org/w/api.php")).withUserAgent(userAgent).build();
    }

    public Wiki getWikipedia(final String lang) {
        return wikipediaCache.computeIfAbsent(lang,
                l -> new Wiki.Builder().withApiEndpoint(HttpUrl.parse("https://" + lang + ".wikipedia.org/w/api.php")).withUserAgent(userAgent).build());
    }

    public Wiki getCommons() {
        return commons;
    }

    /**
     * Wikimedia requires a meaningful User-Agent with contact information, so it links the bot's user page. The link is percent-encoded, as OkHttp rejects non-ASCII header chars.
     */
    static String userAgent(String botUser) {
        HttpUrl userPage = new HttpUrl.Builder().scheme("https").host("uk.wikipedia.org")
                .addPathSegment("wiki").addPathSegment("User:" + botUser.replace(' ', '_')).build();
        return "ukwikitasks (" + userPage + ")";
    }

}
