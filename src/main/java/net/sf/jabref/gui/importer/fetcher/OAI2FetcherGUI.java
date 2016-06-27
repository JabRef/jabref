package net.sf.jabref.gui.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import javax.swing.JOptionPane;

import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OAI2Handler;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fetcher.OAI2Fetcher;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.model.entry.MonthUtil;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class OAI2FetcherGUI extends OAI2Fetcher implements EntryFetcherGUI {

    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_OAI2_ARXIV;
    }

    /**
     * some archives - like ArXiv.org - might expect of you to wait some time
     */
    private boolean shouldWait() {
        return getWaitTime() > 0;
    }

    /**
     * Import an entry from an OAI2 archive. The BibEntry provided has to
     * have the field OAI2_IDENTIFIER_FIELD set to the search string.
     *
     * @param key
     *            The OAI2 key to fetch from ArXiv.
     * @return The imported BibEntry or null if none.
     */
    public BibEntry importOai2Entry(String key) {

        String fixedKey = OAI2Fetcher.fixKey(key);

        String url = constructUrl(fixedKey);
        try {
            URL oai2Url = new URL(url);
            HttpURLConnection oai2Connection = (HttpURLConnection) oai2Url.openConnection();
            oai2Connection.setRequestProperty("User-Agent", "JabRef");

            /* create an empty BibEntry and set the oai2identifier field */
            BibEntry bibEntry = new BibEntry(IdGenerator.next(), "article");
            bibEntry.setField(OAI2FetcherGUI.getOai2IdentifierField(), fixedKey);
            DefaultHandler handlerBase = new OAI2Handler(bibEntry);

            try (InputStream inputStream = oai2Connection.getInputStream()) {

                /* parse the result */
                getSaxParser().parse(inputStream, handlerBase);

                /* Correct line breaks and spacing */
                for (String name : bibEntry.getFieldNames()) {
                    bibEntry.setField(name, OAI2Fetcher.correctLineBreaks(bibEntry.getField(name)));
                }

                if (fixedKey.matches("\\d\\d\\d\\d\\..*")) {
                    bibEntry.setField("year", "20" + fixedKey.substring(0, 2));

                    int monthNumber = Integer.parseInt(fixedKey.substring(2, 4));
                    MonthUtil.Month month = MonthUtil.getMonthByNumber(monthNumber);
                    if (month.isValid()) {
                        bibEntry.setField("month", month.bibtexFormat);
                    }
                }
            }
            return bibEntry;
        } catch (IOException e) {
            getStatus().showMessage(Localization.lang("An Exception occurred while accessing '%0'", url) + "\n\n" + e,
                    getTitle(), JOptionPane.ERROR_MESSAGE);
        } catch (SAXException e) {
            getStatus().showMessage(
                    Localization.lang("An SAXException occurred while parsing '%0':", url) + "\n\n" + e.getMessage(),
                    getTitle(), JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException e) {
            getStatus().showMessage(
                    Localization.lang("Error while fetching from %0", "OAI2 source (" + url + "):") + "\n\n"
                            + e.getMessage() + "\n\n" + Localization
                                    .lang("Note: A full text search is currently not supported for %0", getTitle()),
                    getTitle(), JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    @Override
    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter statusOP) {

        setStatus(statusOP);

        try {
            setContinue(true);

            /* multiple keys can be delimited by ; or space */
            String[] keys = query.replace(" ", ";").split(";");
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];

                /*
                 * some archives - like arxive.org - might expect of you to wait
                 * some time
                 */
                if (shouldWait() && (getLastCall() != null)) {

                    long elapsed = new Date().getTime() - getLastCall().getTime();

                    while (elapsed < getWaitTime()) {
                        getStatus().setStatus(
                                Localization.lang("Waiting for ArXiv...") + ((getWaitTime() - elapsed) / 1000) + " s");
                        Thread.sleep(1000);
                        elapsed = new Date().getTime() - getLastCall().getTime();
                    }
                }

                getStatus().setStatus(Localization.lang("Processing %0", key));

                /* the cancel button has been hit */
                if (!isShouldContinue()) {
                    break;
                }

                /* query the archive and load the results into the BibEntry */
                BibEntry be = importOai2Entry(key);

                if (shouldWait()) {
                    setLastCall(new Date());
                }

                /* add the entry to the inspection dialog */
                if (be != null) {
                    dialog.addEntry(be);
                }

                /* update the dialogs progress bar */
                dialog.setProgress(i + 1, keys.length);
            }

            return true;
        } catch (Exception e) {
            getStatus().setStatus(Localization.lang("Error while fetching from %0", "OAI2"));
            getLogger().error("Error while fetching from OAI2", e);
        }
        return false;
    }

}
