package org.wikipedia.kanzatbot;

import okhttp3.HttpUrl;
import org.fastily.jwiki.core.Wiki;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class WikiFactory {

    private final ConcurrentMap<String, Wiki> wikipediaCache = new ConcurrentHashMap<>();
    private final Wiki commons = new Wiki.Builder().withApiEndpoint(HttpUrl.parse("https://commons.wikimedia.org/w/api.php")).build();

    public Wiki getWikipedia(final String lang) {
        return wikipediaCache.computeIfAbsent(lang,
                l -> new Wiki.Builder().withApiEndpoint(HttpUrl.parse("https://" + lang + ".wikipedia.org/w/api.php")).build());
    }

    public Wiki getCommons() {
        return commons;
    }

}
