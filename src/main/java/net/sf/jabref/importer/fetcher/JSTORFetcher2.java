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
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSTORFetcher2 implements EntryFetcher {

    private static final String CANCELLED = "__CANCELLED__";
    private static final int MAX_PAGES_TO_LOAD = 8;
    protected static int MAX_REFS = 7 * 25;
    private static final int REFS_PER_PAGE = 25; // This is the current default of JSTOR;
    private static final String JSTOR_URL = "http://www.jstor.org";
    private static final String SEARCH_URL = JSTORFetcher2.JSTOR_URL + "/action/doBasicSearch?Query=";
    private static final String SEARCH_URL_END = "&x=0&y=0&wc=on";
    private static final String SINGLE_CIT_ENC =
            //"http://www.jstor.org/action/exportSingleCitation?singleCitation=true&suffix=";
            "http://www.jstor.org/action/exportSingleCitation?singleCitation=true&doi=10.2307/";
    // suffix doesn't work anymore (March 2013), changed to doi=10.2307/citations but only if it a doi
    // to be improved...

    //"http%3A%2F%2Fwww.jstor.org%2Faction%2FexportSingleCitation%3FsingleCitation"
    //+"%3Dtrue%26suffix%3D";
    private static final Pattern idPattern = Pattern.compile(
            "<a class=\"title\" href=\"/stable/(\\d+)\\?");
    private static final Pattern numberofhits = Pattern.compile(
            "<span id=\"NumberOfHits\" name=\"(\\d+)\"");
    private static final Pattern nextPagePattern = Pattern.compile(
            "<a href=\"(.*)\">Next&nbsp;&raquo;");
    private static final String noAccessIndicator = "We do not recognize you as having access to JSTOR";
    private boolean stopFetching;
    private boolean noAccessFound;


    @Override
    public String getHelpPage() {
        return "JSTOR";
    }

    @Override
    public JPanel getOptionsPanel() {
        // No Options panel
        return null;
    }

    @Override
    public String getTitle() {
        return "JSTOR";
    }

    @Override
    public void stopFetching() {
        stopFetching = true;
        noAccessFound = false;
    }

    @Override
    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter status) {
        stopFetching = false;
        try {
            List<String> citations = getCitations(query, dialog, status);
            //System.out.println("JSTORFetcher2 processQuery within list");
            if (citations == null) {
                return false;
            }
            //System.out.println("JSTORFetcher2 processQuery after false citations=" + citations);
            if (citations.isEmpty()) {
                if (!noAccessFound) {
                    status.showMessage(Localization.lang("No entries found for the search string '%0'",
                                    query),
                            Localization.lang("Search %0", "JSTOR"), JOptionPane.INFORMATION_MESSAGE);
                } else {
                    status.showMessage(Localization.lang("No entries found. It looks like you do not have access to search JStor.",
                                    query),
                            Localization.lang("Search %0", "JSTOR"), JOptionPane.INFORMATION_MESSAGE);
                }
                return false;
            }

            int i = 0;
            for (String cit : citations) {
                if (stopFetching) {
                    break;
                }
                getSingleCitation(cit).ifPresent(dialog::addEntry);
                dialog.setProgress(++i, citations.size());
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            status.showMessage(Localization.lang("Error while fetching from JSTOR") + ": " + e.getMessage());
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
    private List<String> getCitations(String query, ImportInspector dialog, OutputPrinter status) throws IOException {
        String urlQuery;
        ArrayList<String> ids = new ArrayList<>();
        try {
            urlQuery = JSTORFetcher2.SEARCH_URL + URLEncoder.encode(query, StandardCharsets.UTF_8.name())
                    + JSTORFetcher2.SEARCH_URL_END;
            int count = 1;
            String[] numberOfRefs = new String[2];
            int refsRequested;
            int numberOfPagesRequested = JSTORFetcher2.MAX_PAGES_TO_LOAD;

            String nextPage;
            while ((count <= Math.min(JSTORFetcher2.MAX_PAGES_TO_LOAD, numberOfPagesRequested))
                    && ((nextPage = getCitationsFromUrl(urlQuery, ids, count, numberOfRefs, dialog, status)) != null)) {
                // If user has cancelled the import, return null to signal this:
                if ((count == 1) && nextPage.equals(JSTORFetcher2.CANCELLED)) {
                    return null;
                }
                //System.out.println("JSTORFetcher2 getCitations numberofrefs=" + numberOfRefs[0]);
                //System.out.println("JSTORFetcher2 getCitations numberofrefs=" + " refsRequested=" + numberOfRefs[1]);
                refsRequested = Integer.valueOf(numberOfRefs[1]);
                //System.out.println("JSTORFetcher2 getCitations refsRequested=" + Integer.valueOf(refsRequested));
                numberOfPagesRequested = ((refsRequested - 1 - ((refsRequested - 1) % JSTORFetcher2.REFS_PER_PAGE)) / JSTORFetcher2.REFS_PER_PAGE) + 1;
                //System.out.println("JSTORFetcher2 getCitations numberOfPagesRequested=" + Integer.valueOf(numberOfPagesRequested));
                urlQuery = nextPage;
                //System.out.println("JSTORFetcher2 getcitations count=" + Integer.valueOf(count) + " ids=" + ids);
                count++;
            }
            return ids;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String getCitationsFromUrl(String urlQuery, List<String> ids, int count,
                                       String[] numberOfRefs, ImportInspector dialog, OutputPrinter status) throws IOException {
        URL url = new URL(urlQuery);
        URLDownload ud = new URLDownload(url);

        String cont = ud.downloadToString();
        String entirePage = cont;
        String pageEntire = cont;

        int countOfRefs = 0;
        int refsRequested;

        if (count == 1) { //  Readin the numberofhits (only once)
            Matcher mn = JSTORFetcher2.numberofhits.matcher(pageEntire);
            if (mn.find()) {
                //System.out.println("JSTORFetcher2 getCitationsFromUrl numberofhits=" + mn.group(1));
                numberOfRefs[0] = mn.group(1);
                countOfRefs = Integer.valueOf(numberOfRefs[0]);
                //System.out.println("JSTORFetcher2 getCitationsFromUrl numberofrefs[0]=" + Integer.valueOf(numberOfRefs[0]));
            } else {
                //System.out.println("JSTORFetcher2 getCitationsFromUrl cant find numberofhits=");
                numberOfRefs[0] = "0";
            }
            while (true) {
                String strCount = JOptionPane.showInputDialog(Localization.lang("References found")
                        + ": " + countOfRefs + "  "
                        + Localization.lang("Number of references to fetch?"), Integer.toString(countOfRefs));

                if (strCount == null) {
                    status.setStatus(Localization.lang("JSTOR import cancelled"));
                    return JSTORFetcher2.CANCELLED;
                }

                try {
                    numberOfRefs[1] = strCount.trim();
                    refsRequested = Integer.parseInt(numberOfRefs[1]);
                    break;
                } catch (RuntimeException ex) {
                    status.showMessage(Localization.lang("Please enter a valid number"));
                }
            }
        }
        countOfRefs = Integer.valueOf(numberOfRefs[0]);
        refsRequested = Integer.valueOf(numberOfRefs[1]);

        Matcher m = JSTORFetcher2.idPattern.matcher(cont);

        if (m.find() && ((ids.size() + 1) <= refsRequested)) {
            do {
                ids.add(m.group(1));
                cont = cont.substring(m.end());
                m = JSTORFetcher2.idPattern.matcher(cont);
            } while (m.find() && ((ids.size() + 1) <= refsRequested));
        } else if (entirePage.contains(JSTORFetcher2.noAccessIndicator)) {
            noAccessFound = true;
            return null;
        } else {
            return null;
        }
        m = JSTORFetcher2.nextPagePattern.matcher(entirePage);

        if (m.find()) {
            return JSTORFetcher2.JSTOR_URL + m.group(1);
        } else {
            return null;
        }
    }

    private static Optional<BibEntry> getSingleCitation(String cit) {
        return BibsonomyScraper.getEntry(JSTORFetcher2.SINGLE_CIT_ENC + cit);
    }
}
