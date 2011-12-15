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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CiteSeerXFetcher implements EntryFetcher {

    protected static int MAX_PAGES_TO_LOAD = 8;
    final static String QUERY_MARKER = "___QUERY___";
    final static String URL_START = "http://citeseer.ist.psu.edu";
    final static String SEARCH_URL = URL_START+"/search?q="+QUERY_MARKER
            +"&submit=Search&sort=rlv&t=doc";
    final static Pattern CITE_LINK_PATTERN = Pattern.compile("<a class=\"remove doc_details\" href=\"(.*)\">");

    protected boolean stopFetching = false;

    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        stopFetching = false;
        try {
            List<String> citations = getCitations(query);
            for (String citation : citations) {
                if (stopFetching)
                    break;
                BibtexEntry entry = getSingleCitation(citation);
                //BibtexEntry entry = BibsonomyScraper.getEntry(citation);

                //dialog.setProgress(++i, citations.size());
                if (entry != null)
                    inspector.addEntry(entry);
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getTitle() {
        return "CiteSeerX";
    }

    public String getKeyName() {
        return "CiteSeerX";
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
        //System.out.println(cont);
        Matcher m = CITE_LINK_PATTERN.matcher(cont);
        while (m.find()) {
            ids.add(URL_START+m.group(1));
        }

        return null;
    }

    final static String basePattern = "<meta name=\""+QUERY_MARKER+"\" content=\"(.*)\" />";
    final static Pattern titlePattern = Pattern.compile(basePattern.replace(QUERY_MARKER, "citation_title"));
    final static Pattern authorPattern = Pattern.compile(basePattern.replace(QUERY_MARKER, "citation_authors"));
    final static Pattern yearPattern = Pattern.compile(basePattern.replace(QUERY_MARKER, "citation_year"));
    final static Pattern abstractPattern = Pattern.compile("<h3>Abstract</h3>\\s*<p>(.*)</p>");

    protected BibtexEntry getSingleCitation(String urlString) throws IOException {

        URL url = new URL(urlString);
        URLDownload ud = new URLDownload(url);
        ud.setEncoding("UTF8");
        ud.download();

        String cont = ud.getStringContent();

        // Find title, and create entry if we do. Otherwise assume we didn't get an entry:
        Matcher m = titlePattern.matcher(cont);
        if (m.find()) {
            BibtexEntry entry = new BibtexEntry(Util.createNeutralId());
            entry.setField("title", m.group(1));

            // Find authors:
            m = authorPattern.matcher(cont);
            if (m.find()) {
                String authors = m.group(1);
                entry.setField("author", NameListNormalizer.normalizeAuthorList(authors));
            }

            // Find year:
            m = yearPattern.matcher(cont);
            if (m.find())
                entry.setField("year", m.group(1));

            // Find abstract:
            m = abstractPattern.matcher(cont);
            if (m.find())
                entry.setField("abstract", m.group(1));

            return entry;
        }
        else
            return null;

    }

}
