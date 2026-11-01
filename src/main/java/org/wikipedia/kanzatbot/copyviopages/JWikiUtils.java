/*
 * Decompiled with CFR 0.152.
 */
package org.wikipedia.kanzatbot.copyviopages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import org.fastily.jwiki.core.MQuery;
import org.fastily.jwiki.core.WQuery;
import org.fastily.jwiki.core.Wiki;
import org.fastily.jwiki.util.Tuple;

public class JWikiUtils {
    public static HashMap<String, ArrayList<Tuple<String, String>>> globalUsageEnhanced(Wiki wiki, Collection<String> titles, String site, String namespace) {
        HashMap<String, String> pl = new HashMap<String, String>();
        pl.put("gulimit", "max");
        pl.put("gunamespace", namespace);
        pl.put("gusite", site);
        return MQuery.parsePropToDouble(MQuery.getContProp(wiki, titles, WQuery.GLOBALUSAGE, pl, "globalusage"), "title", "wiki");
    }
}

