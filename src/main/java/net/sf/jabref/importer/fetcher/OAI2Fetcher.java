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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.jabref.importer.OutputPrinter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

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

    private final String oai2Host;
    private final String oai2Script;
    private final String oai2MetaDataPrefix;
    private final String oai2PrefixIdentifier;
    private final String oai2ArchiveName;

    private SAXParser saxParser;
    private long waitTime = -1;
    private boolean shouldContinue = true;
    private OutputPrinter status;
    private Date lastCall;


    protected OutputPrinter getStatus() {
        return status;
    }

    protected void setStatus(OutputPrinter status) {
        this.status = status;
    }

    protected Date getLastCall() {
        return lastCall;
    }

    protected void setLastCall(Date lastCall) {
        this.lastCall = lastCall;
    }

    protected static String getOai2IdentifierField() {
        return OAI2_IDENTIFIER_FIELD;
    }

    protected static Log getLogger() {
        return LOGGER;
    }

    protected SAXParser getSaxParser() {
        return saxParser;
    }

    protected long getWaitTime() {
        return waitTime;
    }

    protected boolean isShouldContinue() {
        return shouldContinue;
    }

    protected void setContinue(boolean shouldContinue) {
        this.shouldContinue = shouldContinue;
    }

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

    @Override
    public String getTitle() {
        return "ArXiv.org";
    }

    @Override
    public void stopFetching() {
        shouldContinue = false;
    }
}
