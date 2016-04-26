/*  Copyright (C) 2003-2016 JabRef contributors.
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
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OAI2Handler;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.model.entry.MonthUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * This class can be used to access any archive offering an OAI2 interface. By
 * default it will access ArXiv.org
 *
 * @author Ulrich St&auml;rk
 * @author Christian Kopf
 */
public class OAI2Fetcher implements EntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(OAI2Fetcher.class);
    private static final String OAI2_ARXIV_PREFIXIDENTIFIER = "oai%3AarXiv.org%3A";
    private static final String OAI2_ARXIV_HOST = "export.arxiv.org";
    private static final String OAI2_ARXIV_SCRIPT = "oai2";
    private static final String OAI2_ARXIV_METADATAPREFIX = "arXiv";
    private static final String OAI2_ARXIV_ARCHIVENAME = "ArXiv.org";
    private static final String OAI2_IDENTIFIER_FIELD = "oai2identifier";
    private SAXParser saxParser;
    private final String oai2Host;
    private final String oai2Script;
    private final String oai2MetaDataPrefix;
    private final String oai2PrefixIdentifier;
    private final String oai2ArchiveName;
    private boolean shouldContinue = true;
    private OutputPrinter status;
    private long waitTime = -1;
    private Date lastCall;

    /**
     *
     *
     * @param oai2Host
     *            the host to query without leading http:// and without trailing /
     * @param oai2Script
     *            the relative location of the oai2 interface without leading
     *            and trailing /
     * @param oai2Metadataprefix
     *            the urlencoded metadataprefix
     * @param oai2Prefixidentifier
     *            the urlencoded prefix identifier
     * @param waitTimeMs
     *            Time to wait in milliseconds between query-requests.
     */
    public OAI2Fetcher(String oai2Host, String oai2Script, String oai2Metadataprefix, String oai2Prefixidentifier,
            String oai2ArchiveName, long waitTimeMs) {
        this.oai2Host = oai2Host;
        this.oai2Script = oai2Script;
        this.oai2MetaDataPrefix = oai2Metadataprefix;
        this.oai2PrefixIdentifier = oai2Prefixidentifier;
        this.oai2ArchiveName = oai2ArchiveName;
        this.waitTime = waitTimeMs;
        try {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            saxParser = parserFactory.newSAXParser();
        } catch (ParserConfigurationException | SAXException e) {
            LOGGER.error("Error creating SAXParser for OAI2Fetcher", e);
        }
    }

    /**
     * Default Constructor. The archive queried will be ArXiv.org
     *
     */
    public OAI2Fetcher() {
        this(OAI2Fetcher.OAI2_ARXIV_HOST, OAI2Fetcher.OAI2_ARXIV_SCRIPT, OAI2Fetcher.OAI2_ARXIV_METADATAPREFIX,
                OAI2Fetcher.OAI2_ARXIV_PREFIXIDENTIFIER, OAI2Fetcher.OAI2_ARXIV_ARCHIVENAME, 20000L);
    }

    /**
     * Construct the query URL
     *
     * @param key
     *            The key of the OAI2 entry that the url should point to.
     *
     * @return a String denoting the query URL
     */
    public String constructUrl(String key) {
        String identifier;
        try {
            identifier = URLEncoder.encode(key, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return "";
        }
        return "http://" + oai2Host + "/" + oai2Script + "?" + "verb=GetRecord" + "&identifier=" + oai2PrefixIdentifier
                + identifier + "&metadataPrefix=" + oai2MetaDataPrefix;
    }

    /**
     * some archives - like ArXiv.org - might expect of you to wait some time
     */
    private boolean shouldWait() {
        return waitTime > 0;
    }

    /**
     * Strip subcategories from ArXiv key.
     *
     * @param key The key to fix.
     * @return Fixed key.
     */
    public static String fixKey(String key) {

        String resultingKey = key;
        if (resultingKey.toLowerCase(Locale.ENGLISH).startsWith("arxiv:")) {
            resultingKey = resultingKey.substring(6);
        }

        int dot = resultingKey.indexOf('.');
        int slash = resultingKey.indexOf('/');

        if ((dot > -1) && (dot < slash)) {
            resultingKey = resultingKey.substring(0, dot) + resultingKey.substring(slash, resultingKey.length());
        }

        return resultingKey;
    }

    public static String correctLineBreaks(String s) {
        String result = s.replaceAll("\\n(?!\\s*\\n)", " ");
        result = result.replaceAll("\\s*\\n\\s*", "\n");
        return result.replaceAll(" {2,}", " ").replaceAll("(^\\s*|\\s+$)", "");
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
        /**
         * Fix for problem reported in mailing-list:
         *   https://sourceforge.net/forum/message.php?msg_id=4087158
         */
        String fixedKey = OAI2Fetcher.fixKey(key);

        String url = constructUrl(fixedKey);
        try {
            URL oai2Url = new URL(url);
            HttpURLConnection oai2Connection = (HttpURLConnection) oai2Url.openConnection();
            oai2Connection.setRequestProperty("User-Agent", "JabRef");

            /* create an empty BibEntry and set the oai2identifier field */
            BibEntry be = new BibEntry(IdGenerator.next(), "article");
            be.setField(OAI2Fetcher.OAI2_IDENTIFIER_FIELD, fixedKey);
            DefaultHandler handlerBase = new OAI2Handler(be);

            try (InputStream inputStream = oai2Connection.getInputStream()) {

                /* parse the result */
                saxParser.parse(inputStream, handlerBase);

                /* Correct line breaks and spacing */
                for (String name : be.getFieldNames()) {
                    be.setField(name, OAI2Fetcher.correctLineBreaks(be.getField(name)));
                }

                if (fixedKey.matches("\\d\\d\\d\\d\\..*")) {
                    be.setField("year", "20" + fixedKey.substring(0, 2));

                    int monthNumber = Integer.parseInt(fixedKey.substring(2, 4));
                    MonthUtil.Month month = MonthUtil.getMonthByNumber(monthNumber);
                    if (month.isValid()) {
                        be.setField("month", month.bibtexFormat);
                    }
                }
            }
            return be;
        } catch (IOException e) {
            status.showMessage(Localization.lang("An Exception occurred while accessing '%0'", url) + "\n\n" + e,
                    getTitle(), JOptionPane.ERROR_MESSAGE);
        } catch (SAXException e) {
            status.showMessage(
                    Localization.lang("An SAXException occurred while parsing '%0':", url) + "\n\n" + e.getMessage(),
                    getTitle(), JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException e) {
            status.showMessage(
                    Localization.lang("Error while fetching from %0", "OAI2 source (" + url + "):") + "\n\n"
                            + e.getMessage() + "\n\n" + Localization
                                    .lang("Note: A full text search is currently not supported for %0", getTitle()),
                    getTitle(), JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_OAI2_ARXIV;
    }

    @Override
    public JPanel getOptionsPanel() {
        // we have no additional options
        return null;
    }

    @Override
    public String getTitle() {
        return "ArXiv.org";
    }

    @Override
    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter statusOP) {

        status = statusOP;

        try {
            shouldContinue = true;

            /* multiple keys can be delimited by ; or space */
            String[] keys = query.replace(" ", ";").split(";");
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];

                /*
                 * some archives - like arxive.org - might expect of you to wait
                 * some time
                 */
                if (shouldWait() && (lastCall != null)) {

                    long elapsed = new Date().getTime() - lastCall.getTime();

                    while (elapsed < waitTime) {
                        status.setStatus(
                                Localization.lang("Waiting for ArXiv...") + ((waitTime - elapsed) / 1000) + " s");
                        Thread.sleep(1000);
                        elapsed = new Date().getTime() - lastCall.getTime();
                    }
                }

                status.setStatus(Localization.lang("Processing %0", key));

                /* the cancel button has been hit */
                if (!shouldContinue) {
                    break;
                }

                /* query the archive and load the results into the BibEntry */
                BibEntry be = importOai2Entry(key);

                if (shouldWait()) {
                    lastCall = new Date();
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
            status.setStatus(Localization.lang("Error while fetching from %0", "OAI2"));
            LOGGER.error("Error while fetching from OAI2", e);
        }
        return false;
    }

    @Override
    public void stopFetching() {
        shouldContinue = false;
    }
}
