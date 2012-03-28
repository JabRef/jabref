/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.imports;

import net.sf.jabref.*;
import net.sf.jabref.net.URLDownload;
import net.sf.jabref.util.NameListNormalizer;

import javax.swing.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GoogleScholarFetcher implements EntryFetcher {

    protected static int MAX_PAGES_TO_LOAD = 8;
    final static String QUERY_MARKER = "___QUERY___";
    final static String URL_START = "http://scholar.google.com";
    final static String SEARCH_URL = URL_START+"/scholar?q="+QUERY_MARKER
            +"&amp;hl=en&amp;btnG=Search";

    final static Pattern CITE_LINK_PATTERN = Pattern.compile("<div class=gs_r><h3 class=\"gs_rt\"><a href=\"([^\"]*)\">");
    final static Pattern NEXT_PAGE_PATTERN = Pattern.compile(
            "<a href=\"([^\"]*)\"><span class=\"SPRITE_nav_next\"> </span><br><span style=\".*\">Next</span></a>");

    protected boolean stopFetching = false;

    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        stopFetching = false;
        try {
            List<String> citations = getCitations(query);
            int entriesAdded = 0;
            inspector.setProgress(2, citations.size()+2);
            int i=0;
            for (String citation : citations) {
                if (stopFetching)
                    break;

                BibtexEntry entry = BibsonomyScraper.getEntry(citation);

                inspector.setProgress((++i)+2, citations.size()+2);
                if (entry != null) {
                    inspector.addEntry(entry);
                    entriesAdded++;
                }
            }

            if (entriesAdded < citations.size()) {
                JOptionPane.showMessageDialog(null,
                        Globals.lang("%0 entries were found, but only %1 of these could be resolved.",
                                String.valueOf(citations.size()), String.valueOf(entriesAdded)),
                        Globals.lang("Incomplete search results"), JOptionPane.WARNING_MESSAGE);
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getTitle() {
        return "Google Scholar";
    }

    public String getKeyName() {
        return "Google Scholar";
    }

    public URL getIcon() {
        return GUIGlobals.getIconUrl("www");
    }

    public String getHelpPage() {
        return "CiteSeerHelp.html";
    }

    public JPanel getOptionsPanel() {
        return null;
    }

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
    protected List<String> getCitations(String query) throws IOException {
        String urlQuery;
        ArrayList<String> ids = new ArrayList<String>();
        try {
            urlQuery = SEARCH_URL.replace(QUERY_MARKER, URLEncoder.encode(query, "UTF-8"));
            int count = 1;
            String nextPage = null;
            while (((nextPage = getCitationsFromUrl(urlQuery, ids)) != null)
                    && (count < MAX_PAGES_TO_LOAD)) {
                urlQuery = nextPage;
                count++;
                if (stopFetching)
                    break;
            }
            return ids;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getCitationsFromUrl(String urlQuery, List<String> ids) throws IOException {
        URL url = new URL(urlQuery);
        URLDownload ud = new URLDownload(url);
        ud.download();
        String cont = ud.getStringContent();
        Matcher m = CITE_LINK_PATTERN.matcher(cont);
        while (m.find()) {
            ids.add(m.group(1));
        }

        m = NEXT_PAGE_PATTERN.matcher(cont);
        if (m.find()) {
            System.out.println("NEXT: "+URL_START+m.group(1).replaceAll("&amp;", "&"));
            return URL_START+m.group(1).replaceAll("&amp;", "&");
        }
        else return null;
    }

}
