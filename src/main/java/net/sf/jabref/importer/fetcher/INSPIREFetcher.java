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
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 *
 * This class allows to access the Slac INSPIRE database. It is just a port of the original SPIRES Fetcher.
 *
 * It can either be a GeneralFetcher to pose requests to the database or fetch individual entries.
 *
 * @author Fedor Bezrukov
 * @author Sheer El-Showk
 *
 * @version $Id$
 *
 */
public class INSPIREFetcher implements EntryFetcher {

    private static final String INSPIRE_HOST = "inspirehep.net";

    private static final Log LOGGER = LogFactory.getLog(INSPIREFetcher.class);
    /**
     * Construct the query URL
     *
     * NOTE: we truncate at 1000 returned entries but its likely INSPIRE returns fewer anyway. This shouldn't be a
     * problem since users should probably do more specific searches.
     *
     * @param key The key of the OAI2 entry that the url should point to.
     *
     * @return a String denoting the query URL
     */
    private String constructUrl(String key) {
        String identifier;
        try {
            identifier = URLEncoder.encode(key, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return "";
        }
        // At least 87 characters
        StringBuilder sb = new StringBuilder(87).append("http://").append(INSPIREFetcher.INSPIRE_HOST)
                .append("/search?ln=en&ln=en&p=find+").append(identifier)
                .append("&action_search=Search&sf=&so=d&rm=&rg=1000&sc=0&of=hx");
        LOGGER.debug("Inspire URL: " + sb + "\n");
        return sb.toString();
    }

    /**
     * Constructs a INSPIRE query url from slaccitation field
     *
     * @param slaccitation
     * @return query string
     *
     *         public static String constructUrlFromSlaccitation(String slaccitation) { String cmd = "j"; String key =
     *         slaccitation.replaceAll("^%%CITATION = ", "").replaceAll( ";%%$", ""); if (key.matches("^\\w*-\\w*[ /].*"
     *         )) cmd = "eprint"; try { key = URLEncoder.encode(key, "UTF-8"); } catch (UnsupportedEncodingException e)
     *         { } StringBuffer sb = new StringBuffer("http://").append(INSPIRE_HOST) .append("/");
     *         sb.append("spires/find/hep/www").append("?"); sb.append("rawcmd=find+").append(cmd).append("+");
     *         sb.append(key); return sb.toString(); }
     *
     *         /** Construct an INSPIRE query url from eprint field
     *
     * @param eprint
     * @return query string
     *
     *         public static String constructUrlFromEprint(String eprint) { String key = eprint.replaceAll(" [.*]$",
     *         ""); try { key = URLEncoder.encode(key, "UTF-8"); } catch (UnsupportedEncodingException e) { return ""; }
     *         StringBuffer sb = new StringBuffer("http://").append(INSPIRE_HOST) .append("/");
     *         sb.append("spires/find/hep/www").append("?"); sb.append("rawcmd=find+eprint+"); sb.append(key); return
     *         sb.toString(); }
     */

    /**
     * Import an entry from an OAI2 archive. The BibEntry provided has to have the field OAI2_IDENTIFIER_FIELD set to
     * the search string.
     *
     * @param key The OAI2 key to fetch from ArXiv.
     * @return The imported BibEntry or null if none.
     */
    private BibDatabase importInspireEntries(String key, OutputPrinter frame) {
        String url = constructUrl(key);
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "JabRef");
            InputStream inputStream = conn.getInputStream();

            try (INSPIREBibtexFilterReader reader = new INSPIREBibtexFilterReader(
                    new InputStreamReader(inputStream, Charset.forName("UTF-8")))) {

                ParserResult pr = BibtexParser.parse(reader);

                return pr.getDatabase();
            }
        } catch (RuntimeException | IOException e) {
            frame.showMessage(Localization.lang("An Exception occurred while accessing '%0'", url) + "\n\n" + e,
                    getTitle(), JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }


    /*
     * @see net.sf.jabref.imports.fetcher.EntryFetcher
     */
    @Override
    public String getHelpPage() {
        return "INSPIRE";
    }

    @Override
    public JPanel getOptionsPanel() {
        // we have no additional options
        return null;
    }

    @Override
    public String getTitle() {
        return "INSPIRE";
    }

    @Override
    public void stopFetching() {
        // Do nothing
    }

    /*
     * @see java.lang.Runnable
     */
    @Override
    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter frame) {
        try {
            frame.setStatus(Localization.lang("Fetching entries from Inspire"));
            /* query the archive and load the results into the BibEntry */
            BibDatabase bd = importInspireEntries(query, frame);

            frame.setStatus(Localization.lang("Adding fetched entries"));
            /* add the entry to the inspection dialog */
            if (bd == null) {
                LOGGER.warn("Error while fetching from Inspire");
            } else {
                bd.getEntries().forEach(dialog::addEntry);
            }
            /* inform the inspection dialog, that we're done */
        } catch (Exception e) {
            frame.showMessage(Localization.lang("Error while fetching from %0", "Inspire") + ": " + e.getMessage());
            LOGGER.warn("Error while fetching from Inspire", e);
        }
        return true;
    }
}
