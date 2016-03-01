/*  Copyright (C) 2015-2016 JabRef contributors.
    Copyright (C) 2011 Sascha Hunold.
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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.importer.*;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.DuplicateCheck;

public class DBLPFetcher implements EntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(DBLPFetcher.class);

    private static final String URL_START = "http://www.dblp.org/search/api/";
    private static final String URL_PART1 = "?q=";
    private static final String URL_END = "&h=1000&c=4&f=0&format=json";

    private volatile boolean shouldContinue;
    private String query;
    private final DBLPHelper helper = new DBLPHelper();


    @Override
    public void stopFetching() {
        shouldContinue = false;
    }

    @Override
    public boolean processQuery(String newQuery, ImportInspector inspector,
            OutputPrinter status) {

        final HashMap<String, Boolean> bibentryKnown = new HashMap<>();

        boolean res = false;
        this.query = newQuery;

        shouldContinue = true;

        // we save the duplicate check threshold
        // we need to overcome the "smart" approach of this heuristic
        // and we will set it back afterwards, so maybe someone is happy again
        double saveThreshold = DuplicateCheck.duplicateThreshold;

        try {

            String address = makeSearchURL();
            URL url = new URL(address);
            URLDownload dl = new URLDownload(url);

            String page = dl.downloadToString();

            String[] lines = page.split("\n");
            List<String> bibtexUrlList = new ArrayList<>();
            for (final String line : lines) {
                if (line.startsWith("\"url\"")) {
                    String addr = line.replace("\"url\":\"", "");
                    addr = addr.substring(0, addr.length() - 2);
                    bibtexUrlList.add(addr);
                }
            }

            DuplicateCheck.duplicateThreshold = Double.MAX_VALUE;

            // 2014-11-08
            // DBLP now shows the BibTeX entry using ugly HTML entities
            // but they also offer the download of a bib file
            // we find this in the page which we get from "url"
            // and this bib file is then in "biburl"

            int count = 1;
            for (String urlStr : bibtexUrlList) {
                if (!shouldContinue) {
                    break;
                }

                final URL bibUrl = new URL(urlStr);

                final String bibtexHTMLPage = new URLDownload(bibUrl).downloadToString();

                final String[] htmlLines = bibtexHTMLPage.split("\n");

                for (final String line : htmlLines) {
                    if (line.contains("biburl")) {
                        int sidx = line.indexOf('{');
                        int eidx = line.indexOf('}');
                        // now we take everything within the curly braces
                        String bibtexUrl = line.substring(sidx + 1, eidx);

                        // we do not access dblp.uni-trier.de as they will complain
                        bibtexUrl = bibtexUrl.replace("dblp.uni-trier.de", "www.dblp.org");

                        final URL bibFileURL = new URL(bibtexUrl);
                        final String bibtexPage = new URLDownload(bibFileURL).downloadToString();

                        Collection<BibEntry> bibtexEntries = BibtexParser.fromString(bibtexPage);

                        for (BibEntry be : bibtexEntries) {

                            if (!bibentryKnown.containsKey(be.getCiteKey())) {

                                inspector.addEntry(be);
                                bibentryKnown.put(be.getCiteKey(), true);
                            }

                        }
                    }
                }

                inspector.setProgress(count, bibtexUrlList.size());
                count++;
            }


            // everything went smooth
            res = true;

        } catch (IOException e) {
            LOGGER.warn("Communcation problems", e);
            status.showMessage(e.getMessage());
        } finally {
            // Restore the threshold
            DuplicateCheck.duplicateThreshold = saveThreshold;
        }

        return res;
    }

    private String makeSearchURL() {
        StringBuilder sb = new StringBuilder(DBLPFetcher.URL_START).append(DBLPFetcher.URL_PART1);
        String cleanedQuery = helper.cleanDBLPQuery(query);
        sb.append(cleanedQuery).append(DBLPFetcher.URL_END);
        return sb.toString();
    }

    @Override
    public String getTitle() {
        return "DBLP";
    }

    @Override
    public String getHelpPage() {
        return null;
    }

    @Override
    public JPanel getOptionsPanel() {
        return null;
    }

}
