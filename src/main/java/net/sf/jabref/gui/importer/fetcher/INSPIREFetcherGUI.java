package net.sf.jabref.gui.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.swing.JOptionPane;

import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fetcher.INSPIREFetcher;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;

public class INSPIREFetcherGUI extends INSPIREFetcher implements EntryFetcherGUI {

    /*
     * @see net.sf.jabref.imports.fetcher.EntryFetcher
     */
    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_INSPIRE;
    }

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
        StringBuilder sb = new StringBuilder(87).append("http://").append(getInspireHost())
                .append("/search?ln=en&ln=en&p=find+").append(identifier)
                .append("&action_search=Search&sf=&so=d&rm=&rg=1000&sc=0&of=hx");
        getLogger().debug("Inspire URL: " + sb + "\n");
        return sb.toString();
    }

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
                getLogger().warn("Error while fetching from Inspire");
            } else {
                bd.getEntries().forEach(dialog::addEntry);
            }
            /* inform the inspection dialog, that we're done */
        } catch (Exception e) {
            frame.showMessage(Localization.lang("Error while fetching from %0", "Inspire") + ": " + e.getMessage());
            getLogger().warn("Error while fetching from Inspire", e);
        }
        return true;
    }

}
