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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.jabref.importer.*;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.logic.l10n.Localization;

/**
 *
 * This class allows to access the Slac SPIRES database.
 *
 * It can either be a GeneralFetcher to pose requests to the database or fetch
 * individual entries.
 *
 * @author Fedor Bezrukov
 *
 * @version $Id$
 *
 */
public class SPIRESFetcher implements EntryFetcher {

    private static final String spiresHost = "www-spires.slac.stanford.edu";


    /**
     * Construct the query URL
     *
     * @param key The key of the OAI2 entry that the url should point to.
     *
     * @return a String denoting the query URL
     */
    private static String constructUrl(String key) {
        String identifier;
        try {
            identifier = URLEncoder.encode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
        return "http://" + SPIRESFetcher.spiresHost + "/" + "spires/find/hep/www" + "?" + "rawcmd=find+" + identifier + "&FORMAT=WWWBRIEFBIBTEX&SEQUENCE=";
    }

    /**
     * Constructs a SPIRES query url from slaccitation field
     *
     * @param slaccitation
     * @return query string
     */
    public static String constructUrlFromSlaccitation(String slaccitation) {
        String cmd = "j";
        String key = slaccitation.replaceAll("^%%CITATION = ", "").replaceAll(
                ";%%$", "");
        if (key.matches("^\\w*-\\w*[ /].*")) {
            cmd = "eprint";
        }
        try {
            key = URLEncoder.encode(key, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            // Ignored
        }
        return "http://" + SPIRESFetcher.spiresHost + "/" + "spires/find/hep/www" + "?" + "rawcmd=find+" + cmd + "+" + key;
    }

    /**
     * Construct an SPIRES query url from eprint field
     *
     * @param eprint
     * @return query string
     */
    public static String constructUrlFromEprint(String eprint) {
        String key = eprint.replaceAll(" [.*]$", "");
        try {
            key = URLEncoder.encode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
        return "http://" + SPIRESFetcher.spiresHost + "/" + "spires/find/hep/www" + "?" + "rawcmd=find+eprint+" + key;
    }

    /**
     * Import an entry from an OAI2 archive. The BibtexEntry provided has to
     * have the field OAI2_IDENTIFIER_FIELD set to the search string.
     *
     * @param key
     *            The OAI2 key to fetch from ArXiv.
     * @return The imnported BibtexEntry or null if none.
     */
    private BibtexDatabase importSpiresEntries(String key, OutputPrinter frame) {
        String url = constructUrl(key);
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "Jabref");
            InputStream inputStream = conn.getInputStream();

            try (SPIRESBibtexFilterReader reader = new SPIRESBibtexFilterReader(new InputStreamReader(inputStream))) {
                ParserResult pr = BibtexParser.parse(reader);
                return pr.getDatabase();
            }
        } catch (IOException e) {
            frame.showMessage(Localization.lang("An Exception occurred while accessing '%0'", url) + "\n\n" + e,
                    getTitle(), JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException e) {
            frame.showMessage(
                    Localization.lang("An Error occurred while fetching from SPIRES source (%0):", url)
                            + "\n\n" + e.getMessage(),
                    getTitle(), JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    // public void addSpiresURL(BibtexEntry entry) {
    // String url = "http://"+spiresHost+"/spires/find/hep/www?texkey+";
    // url = url+entry.getCiteKey();
    // entry.setField("url", url);
    // }
    //
    // public void addSpiresURLtoDatabase(BibtexDatabase db) {
    // Iterator<BibtexEntry> iter = db.getEntries().iterator();
    // while (iter.hasNext())
    // addSpiresURL(iter.next());
    // }

    /*
     * @see net.sf.jabref.imports.fetcher.EntryFetcher
     */
    @Override
    public String getHelpPage() {
        return "Spires.html";
    }

    @Override
    public JPanel getOptionsPanel() {
        // we have no additional options
        return null;
    }

    @Override
    public String getTitle() {
        return Localization.menuTitle("Fetch_SPIRES");
    }

    /*
     * @see net.sf.jabref.gui.ImportInspectionDialog.CallBack
     */
    public void cancelled() {
        // Nothing
    }

    public void done(int entriesImported) {
        // Nothing
    }

    @Override
    public void stopFetching() {
        // Nothing
    }

    /*
     * @see java.lang.Runnable
     */
    @Override
    public boolean processQuery(String query, ImportInspector dialog,
            OutputPrinter frame) {
        try {
            frame.setStatus("Fetching entries from Spires");
            /* query the archive and load the results into the BibtexEntry */
            BibtexDatabase bd = importSpiresEntries(query, frame);

            /* addSpiresURLtoDatabase(bd); */

            frame.setStatus("Adding fetched entries");
            /* add the entry to the inspection dialog */
            if (bd.getEntryCount() > 0) {
                for (BibtexEntry entry : bd.getEntries()) {
                    dialog.addEntry(entry);
                }
            }

            /* update the dialogs progress bar */
            // dialog.setProgress(i + 1, keys.length);
            /* inform the inspection dialog, that we're done */
        } catch (Exception e) {
            frame.showMessage(Localization.lang("Error while fetching from Spires: ")
                    + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }
}
