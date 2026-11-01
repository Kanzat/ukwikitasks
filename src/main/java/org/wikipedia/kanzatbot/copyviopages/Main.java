/*
 * Decompiled with CFR 0.152.
 */
package org.wikipedia.kanzatbot.copyviopages;

import okhttp3.HttpUrl;
import org.fastily.jwiki.core.Wiki;
import org.wikipedia.kanzatbot.auth.Auth;
import org.wikipedia.kanzatbot.copyvio.CopyvioPages;

public class Main {
    public static Wiki ukWiki;
    public static Wiki commonsWiki;

    public static void main(String[] args2) {
        if (args2.length != 1) {
            throw new IllegalArgumentException("Expected 1 argument (path to credentials file)");
        }
        Auth.credentialsFile = args2[0];
        ukWiki = new Wiki.Builder().withApiEndpoint(HttpUrl.parse("https://uk.wikipedia.org/w/api.php")).build();
        commonsWiki = new Wiki.Builder().withApiEndpoint(HttpUrl.parse("https://commons.wikimedia.org/w/api.php")).build();
        Auth.auth(ukWiki);
        CopyvioPages.update(ukWiki, "\u0412\u0456\u043a\u0456\u043f\u0435\u0434\u0456\u044f:\u0421\u0442\u0430\u0442\u0442\u0456 \u0437 \u043c\u043e\u0436\u043b\u0438\u0432\u0438\u043c \u043f\u043e\u0440\u0443\u0448\u0435\u043d\u043d\u044f\u043c \u0430\u0432\u0442\u043e\u0440\u0441\u044c\u043a\u0438\u0445 \u043f\u0440\u0430\u0432");
    }
}

