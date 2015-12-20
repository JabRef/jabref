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

import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLDownload;

import javax.swing.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScienceDirectFetcher implements EntryFetcher {

    private static final int MAX_PAGES_TO_LOAD = 8;
    private static final String WEBSITE_URL = "http://www.sciencedirect.com";
    private static final String SEARCH_URL = ScienceDirectFetcher.WEBSITE_URL + "/science/quicksearch?query=";

    private static final String linkPrefix = "http://www.sciencedirect.com/science?_ob=ArticleURL&";
    private static final Pattern linkPattern = Pattern.compile(
            "<a href=\"" +
                    ScienceDirectFetcher.linkPrefix.replaceAll("\\?", "\\\\?") +
            "([^\"]+)\"\"");

    protected static final Pattern nextPagePattern = Pattern.compile(
            "<a href=\"(.*)\">Next &gt;");

    private boolean stopFetching;


    @Override
    public String getHelpPage() {
        return "ScienceDirect.html";
    }

    @Override
    public JPanel getOptionsPanel() {
        // No Options panel
        return null;
    }

    @Override
    public String getTitle() {
        return Localization.menuTitle("Search ScienceDirect");
    }

    @Override
    public void stopFetching() {
        stopFetching = true;
    }

    @Override
    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter status) {
        stopFetching = false;
        try {
            List<String> citations = getCitations(query);
            if (citations == null) {
                return false;
            }
            if (citations.isEmpty()) {
                status.showMessage(Localization.lang("No entries found for the search string '%0'",
                        query),
                        Localization.lang("Search ScienceDirect"), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }

            int i = 0;
            for (String cit : citations) {
                if (stopFetching) {
                    break;
                }
                BibEntry entry = BibsonomyScraper.getEntry(cit);
                if (entry != null) {
                    dialog.addEntry(entry);
                }
                dialog.setProgress(++i, citations.size());
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            status.showMessage(Localization.lang("Error while fetching from ScienceDirect") + ": " + e.getMessage());
        }
        return false;
    }

    /**
     *
     * @param query
     *            The search term to query JStor for.
     * @return a list of IDs
     * @throws java.io.IOException
     */
    private static List<String> getCitations(String query) throws IOException {
        String urlQuery;
        ArrayList<String> ids = new ArrayList<>();
        try {
            urlQuery = ScienceDirectFetcher.SEARCH_URL + URLEncoder.encode(query, StandardCharsets.UTF_8.name());
            int count = 1;
            String nextPage;
            while (((nextPage = getCitationsFromUrl(urlQuery, ids)) != null)
                    && (count < ScienceDirectFetcher.MAX_PAGES_TO_LOAD)) {
                urlQuery = nextPage;
                count++;
            }
            return ids;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getCitationsFromUrl(String urlQuery, List<String> ids) throws IOException {
        URL url = new URL(urlQuery);
        String cont = new URLDownload(url).downloadToString();
        //String entirePage = cont;
        Matcher m = ScienceDirectFetcher.linkPattern.matcher(cont);
        if (m.find()) {
            while (m.find()) {
                ids.add(ScienceDirectFetcher.linkPrefix + m.group(1));
                cont = cont.substring(m.end());
                m = ScienceDirectFetcher.linkPattern.matcher(cont);
            }
        }

        else {
            return null;
        }
        /*m = nextPagePattern.matcher(entirePage);
        if (m.find()) {
            String newQuery = WEBSITE_URL +m.group(1);
            return newQuery;
        }
        else*/
        return null;
    }

}
