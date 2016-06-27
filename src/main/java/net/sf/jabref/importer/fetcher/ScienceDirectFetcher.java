/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.importer.fetcher;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.logic.net.URLDownload;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * The current ScienceDirect fetcher implementation does no longer work
 *
 */
@Deprecated
public class ScienceDirectFetcher implements EntryFetcher {

    private static final String SCIENCE_DIRECT = "ScienceDirect";

    private static final Log LOGGER = LogFactory.getLog(ScienceDirectFetcher.class);

    private static final int MAX_PAGES_TO_LOAD = 8;
    private static final String WEBSITE_URL = "http://www.sciencedirect.com";
    private static final String SEARCH_URL = ScienceDirectFetcher.WEBSITE_URL + "/science/quicksearch?query=";

    private static final String LINK_PREFIX = "http://www.sciencedirect.com/science?_ob=ArticleURL&";
    private static final Pattern LINK_PATTERN = Pattern
            .compile("<a href=\"" + ScienceDirectFetcher.LINK_PREFIX.replaceAll("\\?", "\\\\?") + "([^\"]+)\"\"");

    private boolean stopFetching;


    protected boolean isStopFetching() {
        return stopFetching;
    }

    protected void setStopFetching(boolean stopFetching) {
        this.stopFetching = stopFetching;
    }

    protected static String getScienceDirect() {
        return SCIENCE_DIRECT;
    }

    protected static Log getLogger() {
        return LOGGER;
    }

    @Override
    public String getTitle() {
        return "ScienceDirect";
    }

    @Override
    public void stopFetching() {
        stopFetching = true;
    }


    /**
     *
     * @param query
     *            The search term to query JStor for.
     * @return a list of IDs
     * @throws java.io.IOException
     */
    protected static List<String> getCitations(String query) throws IOException {
        String urlQuery;
        List<String> ids = new ArrayList<>();
        urlQuery = ScienceDirectFetcher.SEARCH_URL + URLEncoder.encode(query, StandardCharsets.UTF_8.name());
        int count = 1;
        String nextPage;
        while (((nextPage = getCitationsFromUrl(urlQuery, ids)) != null)
                && (count < ScienceDirectFetcher.MAX_PAGES_TO_LOAD)) {
            urlQuery = nextPage;
            count++;
        }
        return ids;
    }

    private static String getCitationsFromUrl(String urlQuery, List<String> ids) throws IOException {
        String cont = new URLDownload(urlQuery).downloadToString();
        Matcher m = ScienceDirectFetcher.LINK_PATTERN.matcher(cont);
        if (m.find()) {
            while (m.find()) {
                ids.add(ScienceDirectFetcher.LINK_PREFIX + m.group(1));
                cont = cont.substring(m.end());
                m = ScienceDirectFetcher.LINK_PATTERN.matcher(cont);
            }
        }

        else {
            return null;
        }
        return null;
    }

}
