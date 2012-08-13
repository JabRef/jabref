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

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.OutputPrinter;
import net.sf.jabref.net.URLDownload;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSTORFetcher2 implements EntryFetcher {

    protected static final String CANCELLED = "__CANCELLED__";
    protected static int MAX_PAGES_TO_LOAD = 8;
    protected static int MAX_REFS = 7 * 25;
    protected static int REFS_PER_PAGE = 25; // This is the current default of JSTOR;
    protected static final String JSTOR_URL = "http://www.jstor.org";
    protected static final String SEARCH_URL = JSTOR_URL + "/action/doBasicSearch?Query=";
    protected static final String SEARCH_URL_END = "&x=0&y=0&wc=on";
    protected static final String SINGLE_CIT_ENC =
            "http://www.jstor.org/action/exportSingleCitation?singleCitation=true&suffix=";
            //"http%3A%2F%2Fwww.jstor.org%2Faction%2FexportSingleCitation%3FsingleCitation"
            //+"%3Dtrue%26suffix%3D";
    protected static final Pattern idPattern = Pattern.compile(
            "<a class=\"title\" href=\"/stable/(\\d+)\\?");
    protected static final Pattern numberofhits = Pattern.compile(
            "<span id=\"NumberOfHits\" name=\"(\\d+)\"");
    protected static final Pattern nextPagePattern = Pattern.compile(
            "<a href=\"(.*)\">Next&nbsp;&raquo;");
    protected static final String noAccessIndicator = "We do not recognize you as having access to JSTOR";
    protected boolean stopFetching = false;
    protected boolean noAccessFound = false;

    public String getHelpPage() {
        return "JSTOR.html";
    }

    public URL getIcon() {
        return GUIGlobals.getIconUrl("www");
    }

    public String getKeyName() {
        return "JSTOR";
    }

    public JPanel getOptionsPanel() {
        // No Options panel
        return null;
    }

    public String getTitle() {
        return "JSTOR";
    }

    public void stopFetching() {
        stopFetching = true;
        noAccessFound = false;
    }

    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter status) {
        stopFetching = false;
        try {
            List<String> citations = getCitations(query, dialog, status);
            //System.out.println("JSTORFetcher2 processQuery within list");
            if (citations == null) {
                return false;
            }
            //System.out.println("JSTORFetcher2 processQuery after false citations=" + citations);
            if (citations.size() == 0) {
                if (!noAccessFound) {
                    status.showMessage(Globals.lang("No entries found for the search string '%0'",
                        query),
                        Globals.lang("Search JSTOR"), JOptionPane.INFORMATION_MESSAGE);
                } else {
                    status.showMessage(Globals.lang("No entries found. It looks like you do not have access to search JStor.",
                        query),
                        Globals.lang("Search JSTOR"), JOptionPane.INFORMATION_MESSAGE);
                }
                return false;
            }

            int i = 0;
            for (String cit : citations) {
                if (stopFetching) {
                    break;
                }
                BibtexEntry entry = getSingleCitation(cit);
                if (entry != null) {
                    dialog.addEntry(entry);
                }
                dialog.setProgress(++i, citations.size());
            }

            return true;
            
        } catch (IOException e) {
            e.printStackTrace();
            status.showMessage(Globals.lang("Error while fetching from JSTOR") + ": " + e.getMessage());
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
    protected List<String> getCitations(String query, ImportInspector dialog, OutputPrinter status) throws IOException {
        String urlQuery;
        ArrayList<String> ids = new ArrayList<String>();
        try {
            urlQuery = SEARCH_URL + URLEncoder.encode(query, "UTF-8") + SEARCH_URL_END;
            int count = 1;
            String numberOfRefs[] = new String[2];
            int refsRequested = 0;
            int numberOfPagesRequested = MAX_PAGES_TO_LOAD;

            String nextPage = null;
            while ((count <= Math.min(MAX_PAGES_TO_LOAD, numberOfPagesRequested))
                    && ((nextPage = getCitationsFromUrl(urlQuery, ids, count, numberOfRefs, dialog, status)) != null)) {
                // If user has cancelled the import, return null to signal this:
                if ((count == 1) && (nextPage.equals(CANCELLED)))
                    return null;
                //System.out.println("JSTORFetcher2 getCitations numberofrefs=" + numberOfRefs[0]);
                //System.out.println("JSTORFetcher2 getCitations numberofrefs=" + " refsRequested=" + numberOfRefs[1]);
                refsRequested = Integer.valueOf(numberOfRefs[1]);
                //System.out.println("JSTORFetcher2 getCitations refsRequested=" + Integer.valueOf(refsRequested));
                numberOfPagesRequested = ((refsRequested -1) - (refsRequested -1) % REFS_PER_PAGE) / REFS_PER_PAGE + 1;
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

    protected String getCitationsFromUrl(String urlQuery, List<String> ids, int count,
            String[] numberOfRefs, ImportInspector dialog, OutputPrinter status) throws IOException {
        URL url = new URL(urlQuery);
        URLDownload ud = new URLDownload(url);
        ud.download();

        String cont = ud.getStringContent();
        String entirePage = cont;
        String pageEntire = ud.getStringContent();

        int countOfRefs = 0;
        int refsRequested = 0;



        if (count == 1) { //  Readin the numberofhits (only once)
            Matcher mn = numberofhits.matcher(pageEntire);
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
                String strCount = JOptionPane.showInputDialog(Globals.lang("References found")
                        + ": " + countOfRefs + "  "
                        + Globals.lang("Number of references to fetch?"), Integer.toString(countOfRefs));

                if (strCount == null) {
                    status.setStatus(Globals.lang("JSTOR import cancelled"));
                    return CANCELLED;
                }

                try {
                    numberOfRefs[1] = strCount.trim();
                    refsRequested = Integer.parseInt(numberOfRefs[1]);
                    break;
                } catch (RuntimeException ex) {
                    status.showMessage(Globals.lang("Please enter a valid number"));
                }
            }
        }
        countOfRefs = Integer.valueOf(numberOfRefs[0]);
        refsRequested = Integer.valueOf(numberOfRefs[1]);
        
        Matcher m = idPattern.matcher(cont);

        if (m.find() && (ids.size() + 1 <= Integer.valueOf(refsRequested)) ) {
            do {
                ids.add(m.group(1));
                cont = cont.substring(m.end());
                m = idPattern.matcher(cont);
            } while (m.find() && (ids.size() + 1 <= Integer.valueOf(refsRequested)));
        } else if (entirePage.indexOf(noAccessIndicator)
                >= 0) {
            noAccessFound = true;
            return null;
        } else {
            return null;
        }
        m = nextPagePattern.matcher(entirePage);


        if (m.find()) {
            String newQuery = JSTOR_URL + m.group(1);
            return newQuery;
        } else {
            return null;
    }
    }

    protected BibtexEntry getSingleCitation(String cit) {
        return BibsonomyScraper.getEntry(SINGLE_CIT_ENC + cit);
    }
}
