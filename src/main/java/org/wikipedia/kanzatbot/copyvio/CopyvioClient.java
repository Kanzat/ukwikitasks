package org.wikipedia.kanzatbot.copyvio;

import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.fastily.jwiki.util.GSONP;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CopyvioClient {

    public static CopyvioResultDto getForTitle(String title) {
        try {
            HttpUrl url = new HttpUrl.Builder().scheme("https")
                    .host("copyvios.toolforge.org").addPathSegment("api.json")
                    .addQueryParameter("version", "1")
                    .addQueryParameter("action", "search")
                    .addQueryParameter("project", "wikipedia")
                    .addQueryParameter("lang", "uk")
                    .addQueryParameter("use_engine", "true")
                    .addQueryParameter("use_links", "true")
                    .addQueryParameter("title", title).build();
            log.info("Requesting {}", url);
            Request request = new Request.Builder().url(url).get().addHeader("User-Agent", "KanzatBot from Ukrainian Wikipedia").build();
            OkHttpClient client = new OkHttpClient.Builder().connectTimeout(45, TimeUnit.SECONDS)
                    .readTimeout(45, TimeUnit.SECONDS).writeTimeout(45, TimeUnit.SECONDS).build();
            String response = client.newCall(request).execute().body().string();
            return GSONP.gson.fromJson(response, CopyvioResultDto.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
