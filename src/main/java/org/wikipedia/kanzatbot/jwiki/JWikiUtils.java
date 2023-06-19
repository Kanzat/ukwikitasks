package org.wikipedia.kanzatbot.jwiki;

import lombok.extern.slf4j.Slf4j;
import org.fastily.jwiki.core.MQuery;
import org.fastily.jwiki.core.NS;
import org.fastily.jwiki.core.WQuery;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.dwrap.Contrib;
import org.fastily.jwiki.dwrap.LogEntry;
import org.fastily.jwiki.util.FL;
import org.fastily.jwiki.util.GSONP;
import org.fastily.jwiki.util.Tuple;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class JWikiUtils {

    public static List<String> getUsers(final Wiki wiki, final List<String> groups, final boolean onlyActive) {
        final String augroup = groups == null || groups.isEmpty() ? null : String.join("|", groups);
        final WQuery.QTemplate ALLUSERS = new WQuery.QTemplate(
                FL.pMap("list", "allusers", "augroup", augroup, "auactiveusers", String.valueOf(onlyActive)),
                "aulimit", "allusers");
        WQuery wq = new WQuery(wiki, ALLUSERS);

        List<String> allUsers = new ArrayList<>();
        while (wq.has()) {
            Stream<String> users = wq.next().listComp("allusers").stream().map(e -> GSONP.getStr(e, "name"));
            allUsers.addAll(FL.toAL(users));
        }
        return allUsers;
    }

    /**
     * Has additional start and end dates.
     */
    public static List<Contrib> getContribs(Wiki wiki, String user, int cap, boolean olderFirst, boolean createdOnly,
                                     final LocalDateTime start, final LocalDateTime end, NS... ns)
    {
        log.info("Fetching contribs of {}", user);

        WQuery wq = new WQuery(wiki, cap, WQuery.USERCONTRIBS).set("ucuser", user);
        if (ns.length > 0)
            wq.set("ucnamespace", FL.pipeFence(FL.toSet(Stream.of(ns).map(e -> "" + e.v))));
        if (olderFirst)
            wq.set("ucdir", "newer");
        if (createdOnly)
            wq.set("ucshow", "new");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.systemDefault());
        wq.set("ucend", formatter.format(start));
        wq.set("ucstart", formatter.format(end));

        ArrayList<Contrib> l = new ArrayList<>();
        while (wq.has())
            l.addAll(FL.toAL(wq.next().listComp("usercontribs").stream().map(jo -> GSONP.gson.fromJson(jo, Contrib.class))));

        return l;
    }

    /**
     * Has additional start and end dates.
     */
    public static List<LogEntry> getLogs(Wiki wiki, String title, String user, String type, int cap,
                                         final LocalDateTime start, final LocalDateTime end) {
        log.info("Fetching log entries -> title: {}, user: {}, type: {}", title, user, type);
        WQuery wq = new WQuery(wiki, cap, WQuery.LOGEVENTS);
        if (title != null)
            wq.set("letitle", title);
        if (user != null)
            wq.set("leuser", wiki.nss(user));
        if (type != null)
            wq.set("letype", type);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneId.systemDefault());
        wq.set("leend", formatter.format(start));
        wq.set("lestart", formatter.format(end));

        ArrayList<LogEntry> l = new ArrayList<>();
        while (wq.has())
            l.addAll(FL.toAL(wq.next().listComp("logevents").stream().map(jo -> GSONP.gson.fromJson(jo, LogEntry.class))));

        return l;
    }

    public static List<RecentChangeEntry> getNewPages(final Wiki wiki, final LocalDateTime start, final LocalDateTime end) {
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
