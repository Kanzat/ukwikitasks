/*
 * Decompiled with CFR 0.152.
 */
package org.wikipedia.kanzatbot.photorequests;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.fastily.jwiki.core.NS;
import org.fastily.jwiki.core.WParser;
import org.fastily.jwiki.core.Wiki;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wikipedia.kanzatbot.auth.Auth;

@Slf4j
@Component
public class PhotoRequests {

    public void update(Wiki ukWiki) {
        ArrayList<String> users = ukWiki.getCategoryMembers("\u041a\u043e\u0440\u0438\u0441\u0442\u0443\u0432\u0430\u0447\u0456, \u0449\u043e \u043c\u043e\u0436\u0443\u0442\u044c \u0437\u0440\u043e\u0431\u0438\u0442\u0438 \u0441\u0432\u0456\u0442\u043b\u0438\u043d\u0443 \u0434\u043b\u044f \u0412\u0456\u043a\u0456\u043f\u0435\u0434\u0456\u0457", NS.USER);
        TreeMap<String, String> userToLocationMap = new TreeMap<String, String>();
        TreeSet<String> noLocationUsers = new TreeSet<String>();
        for (String string : users) {
            String string2 = ukWiki.getPageText(string);
            String userWithoutPrefix = string.substring(string.indexOf(":") + 1);
            WParser.WikiText wikiText = WParser.parseText(ukWiki, string2);
            Optional<WParser.WTemplate> canMakePhotoTemplate = wikiText.getTemplatesR().stream().filter(t -> t.title.equals("\u041c\u043e\u0436\u0443 \u0441\u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0443\u0432\u0430\u0442\u0438") || t.title.endsWith(":\u041c\u043e\u0436\u0443 \u0441\u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0443\u0432\u0430\u0442\u0438")).findFirst();
            if (canMakePhotoTemplate.isPresent()) {
                if (canMakePhotoTemplate.get().has("1")) {
                    String location = canMakePhotoTemplate.get().get("1").toString();
                    userToLocationMap.put(userWithoutPrefix, location);
                    continue;
                }
                noLocationUsers.add(userWithoutPrefix);
                log.error("User {} didn't specify location", (Object)userWithoutPrefix);
                continue;
            }
            log.error("Failed to find {{\u041c\u043e\u0436\u0443 \u0441\u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0443\u0432\u0430\u0442\u0438}}");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\u041f\u0435\u0440\u0435\u043b\u0456\u043a \u043a\u043e\u0440\u0438\u0441\u0442\u0443\u0432\u0430\u0447\u0456\u0432, \u044f\u043a\u0456 \u043c\u043e\u0436\u0443\u0442\u044c \u0437\u0440\u043e\u0431\u0438\u0442\u0438 \u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0456\u044e, \u0432\u0456\u0434\u043f\u043e\u0432\u0456\u0434\u043d\u043e \u0434\u043e \u0448\u0430\u0431\u043b\u043e\u043d\u0443 {{tl|\u041c\u043e\u0436\u0443 \u0441\u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0443\u0432\u0430\u0442\u0438}}. \u0426\u044f \u0441\u0442\u043e\u0440\u0456\u043d\u043a\u0430 \u0434\u043e\u043f\u043e\u043c\u0456\u0436\u043d\u0430 \u0434\u043b\u044f \u0432\u0438\u043a\u043e\u043d\u0430\u043d\u043d\u044f \u0437\u0430\u043f\u0438\u0442\u0456\u0432 \u043d\u0430 \u0437\u043e\u0431\u0440\u0430\u0436\u0435\u043d\u043d\u044f (\u0434\u0438\u0432. [[\u0412\u0456\u043a\u0456\u043f\u0435\u0434\u0456\u044f:\u0417\u0430\u043f\u0438\u0442\u0438 \u043d\u0430 \u0437\u043e\u0431\u0440\u0430\u0436\u0435\u043d\u043d\u044f]]).\n\n");
        sb.append("{| class=\"wikitable sortable\"\n");
        sb.append("|+\n");
        sb.append("! \u041a\u043e\u0440\u0438\u0441\u0442\u0443\u0432\u0430\u0447(\u043a\u0430)\n");
        sb.append("! \u0414\u0435 \u043c\u043e\u0436\u0435 \u0441\u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0443\u0432\u0430\u0442\u0438\n");
        for (Map.Entry entry : userToLocationMap.entrySet()) {
            sb.append("|-\n");
            sb.append("|" + PhotoRequests.formatUser((String)entry.getKey()) + "\n");
            sb.append("|" + (String)entry.getValue() + "\n");
        }
        sb.append("|}\n");
        sb.append("\n");
        String string = noLocationUsers.stream().map(PhotoRequests::formatUser).sorted().collect(Collectors.joining(", "));
        sb.append("\u041a\u043e\u0440\u0438\u0441\u0442\u0443\u0432\u0430\u0447\u0456, \u044f\u043a\u0456 \u043f\u043e\u0441\u0442\u0430\u0432\u0438\u043b\u0438 \u0448\u0430\u0431\u043b\u043e\u043d, \u0430\u043b\u0435 \u043d\u0435 \u0437\u0430\u0437\u043d\u0430\u0447\u0438\u043b\u0438 \u043c\u0456\u0441\u0446\u0435\u0432\u0456\u0441\u0442\u044c: ").append(string);
        ukWiki.edit("User:KanzatBot/Reports/PhotoVolunteersMap", sb.toString(), "\u041e\u043d\u043e\u0432\u043b\u0435\u043d\u043d\u044f \u0456\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0456\u0457");
    }

    private static String formatUser(String user) {
        return "[[User:" + user + "|" + user + "]]";
    }
}

