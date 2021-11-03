package org.wikipedia.kanzatbot.jwiki;

import org.fastily.jwiki.core.MQuery;
import org.fastily.jwiki.core.WQuery;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.Tuple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class JWikiUtils {

    public static HashMap<String, ArrayList<Tuple<String, String>>> globalUsageEnhanced(final Wiki wiki, final Collection<String> titles, final String site, final String namespace) {
        final HashMap<String, String> pl = new HashMap<>();
        pl.put("gulimit", "max");
        pl.put("gunamespace", namespace);
        pl.put("gusite", site);
        return MQuery.parsePropToDouble(MQuery.getContProp(wiki, titles, WQuery.GLOBALUSAGE, pl, "globalusage"), "title", "wiki");
    }

}
