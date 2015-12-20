/*  Copyright (C) 2015 JabRef contributors.
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

import net.sf.jabref.importer.*;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.util.Util;
import net.sf.jabref.bibtex.DuplicateCheck;

public class DBLPFetcher implements EntryFetcher {

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

        try {

            String address = makeSearchURL();
            //System.out.println(address);
            URL url = new URL(address);
            String page = Util.getResults(url);

            //System.out.println(page);
            String[] lines = page.split("\n");
            List<String> bibtexUrlList = new ArrayList<>();
            for (final String line : lines) {
                if (line.startsWith("\"url\"")) {
                    String addr = line.replace("\"url\":\"", "");
                    addr = addr.substring(0, addr.length() - 2);
                    //System.out.println("key address: " + addr);
                    bibtexUrlList.add(addr);
                }
            }

            // we save the duplicate check threshold
            // we need to overcome the "smart" approach of this heuristic
            // and we will set it back afterwards, so maybe someone is happy again
            double saveThreshold = DuplicateCheck.duplicateThreshold;
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

                final String bibtexHTMLPage = Util.getResults(bibUrl);

                final String[] htmlLines = bibtexHTMLPage.split("\n");

                for (final String line : htmlLines) {
                    if (line.contains("biburl")) {
                        int sidx = line.indexOf("{");
                        int eidx = line.indexOf("}");
                        // now we take everything within the curley braces
                        String bibtexUrl = line.substring(sidx + 1, eidx);

                        // we do not access dblp.uni-trier.de as they will complain
                        bibtexUrl = bibtexUrl.replace("dblp.uni-trier.de", "www.dblp.org");

                        final URL bibFileURL = new URL(bibtexUrl);
                        //System.out.println("URL:|"+bibtexUrl+"|");
                        final String bibtexPage = Util.getResults(bibFileURL);

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

            DuplicateCheck.duplicateThreshold = saveThreshold;

            // everything went smooth
            res = true;

        } catch (IOException e) {
            e.printStackTrace();
            status.showMessage(e.getMessage());
        }

        return res;
    }

    private String makeSearchURL() {
        StringBuilder sb = new StringBuilder(DBLPFetcher.URL_START).append(DBLPFetcher.URL_PART1);
        String cleanedQuery = helper.cleanDBLPQuery(query);
        sb.append(cleanedQuery);
        sb.append(DBLPFetcher.URL_END);
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
