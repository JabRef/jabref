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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.logic.l10n.Localization;

/**
 * This class fetches up to 200 citations from JStor by a given search query. It
 * communicates with jstor via HTTP and Cookies. The activeFetcher automates the
 * following steps:
 * <ol>
 * <li>Do a basic search on www.jstor.org</li>
 * <li>Save the first 200 hits</li>
 * <li>Download the saved citations as bibtex</li>
 * <li>Parse it with the BibtexParser</li>
 * <li>Import the BibtexEntrys via the ImportInspectionDialog</li>
 * </ol>
 *
 * @author Juliane Doege, Tobias Langner
 */
public class JSTORFetcher implements EntryFetcher {

    /**
     * cookies can't save more than 200 citations
     */
    private static final int MAX_CITATIONS = 200;

    /**
     * Cookie key for Jstor ticket (authentication)
     */
    private static final String COOKIE_TICKET = "Jstor_Ticket";

    /**
     * location where the ticket is obtained
     *
     */
    private static final String URL_TICKET = "http://www.jstor.org/search";

    /**
     * Cookie key for citations to be fetched
     *
     */
    private static final String COOKIE_CITATIONS = "Jstor_citations0";

    /**
     * location where to obtain the citations cookie
     *
     */
    private static final String URL_BIBTEX = "http://www.jstor.org/browse/citations.txt?exportFormat=bibtex&exportAction=Display&frame=noframe&dpi=3&config=jstor&viewCitations=1&View=View";


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
        // cannot be interrupted
    }

    @Override
    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter status) {

        try {
            // First open a ticket with JStor
            String ticket = openTicket();

            // Then execute the query
            String citations = getCitations(ticket, query);

            // Last retrieve the Bibtex-entries of the citations found
            Collection<BibEntry> entries = getBibtexEntries(ticket, citations);

            if (entries.isEmpty()) {
                status.showMessage(Localization.lang("No entries found for the search string '%0'",
                                query),
                        Localization.lang("Search %0", "JSTOR"), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }

            for (BibEntry entry : entries) {
                dialog.addEntry(entry);
            }
            return true;
        } catch (IOException e) {
            status.showMessage(Localization.lang("Error while fetching from JSTOR") + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Given a ticket an a list of citations, retrieve BibtexEntries from JStor
     *
     * @param ticket
     *            A valid ticket as returned by openTicket()
     * @param citations
     *            A list of citations as returned by getCitations()
     * @return A collection of BibtexEntries parsed from the bibtex returned by
     *         JStor.
     * @throws IOException
     *             Most probably related to a problem connecting to JStor.
     */
    private static Collection<BibEntry> getBibtexEntries(String ticket, String citations)
            throws IOException {
        try {
            URL url = new URL(JSTORFetcher.URL_BIBTEX);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("Cookie", ticket + "; " + citations);
            conn.connect();

            BibtexParser parser = new BibtexParser(new BufferedReader(new InputStreamReader(conn
                    .getInputStream())));
            return parser.parse().getDatabase().getEntries();
        } catch (MalformedURLException e) {
            // Propagate...
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @return a Jstor ticket ID
     * @throws IOException
     */
    private static String openTicket() throws IOException {
        URL url = new URL(JSTORFetcher.URL_TICKET);
        URLConnection conn = url.openConnection();
        return JSTORFetcher.getCookie(JSTORFetcher.COOKIE_TICKET, conn);
    }

    /**
     * requires a valid JStor Ticket ID
     *
     * @param query
     *            The search term to query JStor for.
     * @param ticket
     *            JStor ticket
     * @return cookie value of the key JSTORFetcher.COOKIE_CITATIONS. null if
     *         search is empty or ticket is invalid
     * @throws IOException
     */
    private static String getCitations(String ticket, String query) throws IOException {
        String urlQuery;
        try {
            urlQuery = "http://www.jstor.org/search/BasicResults?hp=" + JSTORFetcher.MAX_CITATIONS +
 "&si=1&gw=jtx&jtxsi=1&jcpsi=1&artsi=1&Query="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8.name()) +
                    "&wc=on&citationAction=saveAll";
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        URL url = new URL(urlQuery);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("Cookie", ticket);
        return JSTORFetcher.getCookie(JSTORFetcher.COOKIE_CITATIONS, conn);
    }

    /**
     * evaluates the 'Set-Cookie'-Header of a HTTP response
     *
     * @param name
     *            key of a cookie value
     * @param conn
     *            URLConnection
     * @return cookie value referenced by the key. null if key not found
     * @throws IOException
     */
    private static String getCookie(String name, URLConnection conn) {

        for (int i = 0;; i++) {
            String headerName = conn.getHeaderFieldKey(i);
            String headerValue = conn.getHeaderField(i);

            if ((headerName == null) && (headerValue == null)) {
                // No more headers
                break;
            }
            if ("Set-Cookie".equals(headerName)) {
                if (headerValue.startsWith(name)) {
                    // several key-value-pairs are separated by ';'
                    StringTokenizer st = new StringTokenizer(headerValue, "; ");
                    while (st.hasMoreElements()) {
                        String token = st.nextToken();
                        if (token.startsWith(name)) {
                            return token;
                        }
                    }
                }
            }

        }
        return null;
    }

}
