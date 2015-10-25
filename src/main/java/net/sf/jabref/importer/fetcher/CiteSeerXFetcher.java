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
import net.sf.jabref.logic.id.IdGenerator;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.logic.util.strings.NameListNormalizer;
import net.sf.jabref.model.entry.BibtexEntry;

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

    private static final int MAX_PAGES_TO_LOAD = 8;
    private static final String QUERY_MARKER = "___QUERY___";
    private static final String URL_START = "http://citeseer.ist.psu.edu";
    private static final String SEARCH_URL = CiteSeerXFetcher.URL_START + "/search?q=" + CiteSeerXFetcher.QUERY_MARKER
            + "&submit=Search&sort=rlv&t=doc";
    private static final Pattern CITE_LINK_PATTERN = Pattern.compile("<a class=\"remove doc_details\" href=\"(.*)\">");

    private boolean stopFetching;


    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        stopFetching = false;
        try {
            List<String> citations = getCitations(query);
            for (String citation : citations) {
                if (stopFetching) {
                    break;
                }
                BibtexEntry entry = getSingleCitation(citation);
                //BibtexEntry entry = BibsonomyScraper.getEntry(citation);

                //dialog.setProgress(++i, citations.size());
                if (entry != null) {
                    inspector.addEntry(entry);
                }
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String getTitle() {
        return "CiteSeerX";
    }

    @Override
    public String getKeyName() {
        return "CiteSeerX";
    }

    @Override
    public String getHelpPage() {
        return "CiteSeerHelp.html";
    }

    @Override
    public JPanel getOptionsPanel() {
        return null;
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
    private List<String> getCitations(String query) throws IOException {
        String urlQuery;
        ArrayList<String> ids = new ArrayList<>();
        try {
            urlQuery = CiteSeerXFetcher.SEARCH_URL.replace(CiteSeerXFetcher.QUERY_MARKER, URLEncoder.encode(query, "UTF-8"));
            int count = 1;
            String nextPage;
            while (((nextPage = getCitationsFromUrl(urlQuery, ids)) != null)
                    && (count < CiteSeerXFetcher.MAX_PAGES_TO_LOAD)) {
                urlQuery = nextPage;
                count++;
                if (stopFetching) {
                    break;
                }
            }
            return ids;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getCitationsFromUrl(String urlQuery, List<String> ids) throws IOException {
        URL url = new URL(urlQuery);
        String cont = new URLDownload(url).downloadToString();
        //System.out.println(cont);
        Matcher m = CiteSeerXFetcher.CITE_LINK_PATTERN.matcher(cont);
        while (m.find()) {
            ids.add(CiteSeerXFetcher.URL_START + m.group(1));
        }

        return null;
    }


    private static final String basePattern = "<meta name=\"" + CiteSeerXFetcher.QUERY_MARKER + "\" content=\"(.*)\" />";
    private static final Pattern titlePattern = Pattern.compile(CiteSeerXFetcher.basePattern.replace(CiteSeerXFetcher.QUERY_MARKER, "citation_title"));
    private static final Pattern authorPattern = Pattern.compile(CiteSeerXFetcher.basePattern.replace(CiteSeerXFetcher.QUERY_MARKER, "citation_authors"));
    private static final Pattern yearPattern = Pattern.compile(CiteSeerXFetcher.basePattern.replace(CiteSeerXFetcher.QUERY_MARKER, "citation_year"));
    private static final Pattern abstractPattern = Pattern.compile("<h3>Abstract</h3>\\s*<p>(.*)</p>");


    private static BibtexEntry getSingleCitation(String urlString) throws IOException {

        URL url = new URL(urlString);
        String cont = new URLDownload(url).downloadToString("UTF8");

        // Find title, and create entry if we do. Otherwise assume we didn't get an entry:
        Matcher m = CiteSeerXFetcher.titlePattern.matcher(cont);
        if (m.find()) {
            BibtexEntry entry = new BibtexEntry(IdGenerator.next());
            entry.setField("title", m.group(1));

            // Find authors:
            m = CiteSeerXFetcher.authorPattern.matcher(cont);
            if (m.find()) {
                String authors = m.group(1);
                entry.setField("author", NameListNormalizer.normalizeAuthorList(authors));
            }

            // Find year:
            m = CiteSeerXFetcher.yearPattern.matcher(cont);
            if (m.find()) {
                entry.setField("year", m.group(1));
            }

            // Find abstract:
            m = CiteSeerXFetcher.abstractPattern.matcher(cont);
            if (m.find()) {
                entry.setField("abstract", m.group(1));
            }

            return entry;
        } else {
            return null;
        }

    }

}
