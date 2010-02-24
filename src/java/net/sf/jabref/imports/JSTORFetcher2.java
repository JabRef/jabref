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

    protected static int MAX_PAGES_TO_LOAD = 8;
    protected static final String JSTOR_URL = "http://www.jstor.org";
    protected static final String SEARCH_URL = JSTOR_URL+"/action/doBasicSearch?Query=";
    protected static final String SEARCH_URL_END = "&x=0&y=0&wc=on";
    protected static final String SINGLE_CIT_ENC =
            "http%3A%2F%2Fwww.jstor.org%2Faction%2FexportSingleCitation%3FsingleCitation"
            +"%3Dtrue%26suffix%3D";
    protected static final String BIBSONOMY_SCRAPER = "http://scraper.bibsonomy.org/service?url=";
    protected static final String BIBSONOMY_SCRAPER_POST = "&format=bibtex";

    protected static final Pattern idPattern = Pattern.compile(
            "<a class=\"title\" href=\"/stable/(\\d+)\\?");

    protected static final Pattern nextPagePattern = Pattern.compile(
            "<a href=\"(.*)\">Next &gt;");

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
        return "Search JSTOR";
    }

    public JPanel getOptionsPanel() {
        // No Options panel
        return null;
    }

    public String getTitle() {
        return Globals.menuTitle("Search JSTOR");
    }

    public void stopFetching() {
        stopFetching = true;
        noAccessFound = false;
    }

    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter status) {
        stopFetching = false;
        try {
            List<String> citations = getCitations(query);
            if (citations == null)
                return false;
            if (citations.size() == 0){
                if (!noAccessFound)
                    status.showMessage(Globals.lang("No entries found for the search string '%0'",
                        query),
                        Globals.lang("Search JSTOR"), JOptionPane.INFORMATION_MESSAGE);
                else {
                    status.showMessage(Globals.lang("No entries found. It looks like you do not have access to search JStor.",
                        query),
                        Globals.lang("Search JSTOR"), JOptionPane.INFORMATION_MESSAGE);
                }
                return false;
            }

            int i=0;
            for (String cit : citations) {
                if (stopFetching)
                    break;
                BibtexEntry entry = getSingleCitation(cit);
                if (entry != null)
                    dialog.addEntry(entry);
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
    protected List<String> getCitations(String query) throws IOException {
        String urlQuery;
        ArrayList<String> ids = new ArrayList<String>();
        try {
            urlQuery = SEARCH_URL + URLEncoder.encode(query, "UTF-8") + SEARCH_URL_END;
            int count = 1;
            String nextPage = null;
            while (((nextPage = getCitationsFromUrl(urlQuery, ids)) != null)
                    && (count < MAX_PAGES_TO_LOAD)) {
                urlQuery = nextPage;
                count++;
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
        String entirePage = cont;

        Matcher m = idPattern.matcher(cont);
        if (m.find()) {
            while (m.find()) {
                ids.add(m.group(1));
                cont = cont.substring(m.end());
                m = idPattern.matcher(cont);
            }
        }
        else if (entirePage.indexOf(noAccessIndicator) >= 0) {
            noAccessFound = true;
            return null;
        }
        else {
            return null;
        }
        m = nextPagePattern.matcher(entirePage);
        if (m.find()) {
            String newQuery = JSTOR_URL+m.group(1);
            return newQuery;
        }
        else
            return null;
    }

    protected BibtexEntry getSingleCitation(String cit) {
        String jstorEntryUrl = SINGLE_CIT_ENC+cit;
        try {
            URL url = new URL(BIBSONOMY_SCRAPER+jstorEntryUrl+BIBSONOMY_SCRAPER_POST);
            URLDownload ud = new URLDownload(url);
            ud.download();
            String bibtex = ud.getStringContent();
            BibtexParser bp = new BibtexParser(new StringReader(bibtex));
            ParserResult pr = bp.parse();
            if ((pr != null) && (pr.getDatabase().getEntryCount() > 0)) {
                return pr.getDatabase().getEntries().iterator().next();
            }
            else return null;
            
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

}