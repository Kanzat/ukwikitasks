package org.wikipedia.kanzatbot.jwiki;

import org.fastily.jwiki.core.MQuery;
import org.fastily.jwiki.core.WQuery;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.FL;
import org.fastily.jwiki.util.GSONP;
import org.fastily.jwiki.util.Tuple;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class JWikiUtils {

    public static List<RecentChangeEntry> getNewPages(final Wiki wiki, final Instant start, final Instant end) {
        final WQuery.QTemplate RECENTCHANGES = new WQuery.QTemplate(
                FL.pMap("list", "recentchanges", "rcprop", "title|timestamp|user|redirect", "rctype", "new", "rcnamespace", "0"), "rclimit",
                "recentchanges");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.systemDefault());
        WQuery wq = new WQuery(wiki, RECENTCHANGES).set("rcend", formatter.format(start));
        wq.set("rcstart", formatter.format(end));

        ArrayList<RecentChangeEntry> l = new ArrayList<>();
        while (wq.has()) {
            Stream<RecentChangeEntry> recentchanges = wq.next().listComp("recentchanges").stream().map(jo -> GSONP.gson.fromJson(jo, RecentChangeEntry.class));
            l.addAll(FL.toAL(recentchanges));
        }

        return l;
    }

    public static HashMap<String, ArrayList<Tuple<String, String>>> globalUsageEnhanced(final Wiki wiki, final Collection<String> titles, final String site, final String namespace) {
        final HashMap<String, String> pl = new HashMap<>();
        pl.put("gulimit", "max");
        pl.put("gunamespace", namespace);
        pl.put("gusite", site);
        return MQuery.parsePropToDouble(MQuery.getContProp(wiki, titles, WQuery.GLOBALUSAGE, pl, "globalusage"), "title", "wiki");
    }

}
