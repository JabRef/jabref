package org.jabref.gui.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.swing.JPanel;

import org.jabref.Globals;
import org.jabref.gui.importer.ImportInspectionDialog;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.ImportInspector;
import org.jabref.logic.importer.OutputPrinter;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.util.INSPIREBibtexFilterReader;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER = LoggerFactory.getLogger(INSPIREFetcher.class);
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
    private BibDatabase importInspireEntries(String key) throws IOException {
        String url = constructUrl(key);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestProperty("User-Agent", "JabRef");
        InputStream inputStream = conn.getInputStream();

        try (INSPIREBibtexFilterReader reader = new INSPIREBibtexFilterReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            ParserResult pr = new BibtexParser(Globals.prefs.getImportFormatPreferences(), Globals.getFileUpdateMonitor()).parse(reader);
            return pr.getDatabase();
        }
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_INSPIRE;
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
    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter status) {
        try {
            status.setStatus(Localization.lang("Fetching entries from Inspire"));
            /* query the archive and load the results into the BibEntry */
            BibDatabase bd = importInspireEntries(query);

            status.setStatus(Localization.lang("Adding fetched entries"));
            /* add the entry to the inspection dialog */
            bd.getEntries().forEach(dialog::addEntry);
            return true;
        } catch (Exception e) {
            LOGGER.error("Error while fetching from " + getTitle(), e);
            ((ImportInspectionDialog)dialog).showErrorMessage(this.getTitle(), e.getLocalizedMessage());
        }
        return false;
    }
}
