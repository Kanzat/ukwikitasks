package org.wikipedia.kanzatbot.patrols;

import lombok.extern.slf4j.Slf4j;
import org.fastily.jwiki.core.Wiki;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class CreateCandidatePatrolsPage {

    public void create(Wiki ukWiki) {
        final String activeUsersPage = ukWiki.getPageText("Користувач:RLutsBot/Активні");
        final int startIndex = activeUsersPage.indexOf("|-");
        final int startIndex2 = activeUsersPage.indexOf("|-", startIndex + 2);
        final int endIndex = activeUsersPage.indexOf("== Див. також ==");
        final String tableContent = activeUsersPage.substring(startIndex2 + 2, endIndex);
        for (String userInfo : tableContent.split(Pattern.quote("|-") + "\r?\n")) {
            try {
                final int startRank = userInfo.indexOf("| ");
                final int startLink = userInfo.indexOf("\n| ", startRank + 1);
                final int startEditCount = userInfo.indexOf("\n|", startLink + 1);
                final int rank = Integer.parseInt(userInfo.substring(startRank + 2, startLink).trim());
                final String userLink = userInfo.substring(startLink + 3, startEditCount).trim();
                final String userName = userLink.substring("[[User:".length(), userLink.indexOf("|")).trim();
                final int editCount = Integer.parseInt(userInfo.substring(startEditCount + 2).trim());
                log.info(rank + " " + userName + " " + editCount);
            } catch (NumberFormatException e) {
                log.warn("ignore the last one: " + userInfo);
            }
        }
    }

}
