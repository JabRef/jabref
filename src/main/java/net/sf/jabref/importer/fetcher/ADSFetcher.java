/* Copyright (C) 2003-2016 JabRef Contributors
 * Copyright (c) 2009, Ryo IGARASHI <rigarash@gmail.com>
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.sf.jabref.importer.fetcher;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import javax.swing.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * This class handles accessing and obtaining BibTeX entry
 * from ADS(The NASA Astrophysics Data System).
 * Fetching using DOI(Document Object Identifier) is only supported.
 *
 * @author Ryo IGARASHI
 */
public class ADSFetcher implements EntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(ADSFetcher.class);


    @Override
    public JPanel getOptionsPanel() {
        return null;
    }

    @Override
    public String getHelpPage() {
        return null;
    }

    @Override
    public String getTitle() {
        return "ADS from ADS-DOI";
    }

    @Override
    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter status) {
        try {
            /* Remove "doi:" scheme identifier */
            query = query.replaceAll("^(doi:|DOI:)", "");
            /* Allow fetching only 1 key */
            String key = query;
            /* Query ADS and load the results into the BibDatabase */
            status.setStatus(Localization.lang("Processing %0", key));
            BibDatabase bd = importADSEntries(key, status);
            if ((bd != null) && bd.hasEntries()) {
                /* Add the entry to the inspection dialog */
                for (BibEntry entry : bd.getEntries()) {
                    importADSAbstract(key, entry, status);
                    dialog.addEntry(entry);
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            status.setStatus(Localization.lang("Error while fetching from %0", "ADS") + ": " + e.getMessage());
            LOGGER.warn("Error while fetching from ADS", e);
        }
        return true;
    }

    @Override
    public void stopFetching() {
        // Do nothing
    }

    private BibDatabase importADSEntries(String key, OutputPrinter status) {
        String url = constructUrl(key);
        try {
            URL ADSUrl = new URL(url + "&data_type=BIBTEX");
            HttpURLConnection ADSConnection = (HttpURLConnection) ADSUrl.openConnection();
            ADSConnection.setRequestProperty("User-Agent", "JabRef");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(ADSConnection.getInputStream(), Charset.forName("ISO-8859-1")))) {
                ParserResult pr = BibtexParser.parse(reader);
                return pr.getDatabase();
            }
        } catch (FileNotFoundException e) {
            status.showMessage(
                    Localization.lang("'%0' is not a valid ADS bibcode.", key) + "\n\n" + Localization
                            .lang("Note: A full text search is currently not supported for %0", getTitle()),
                    getTitle(), JOptionPane.ERROR_MESSAGE);
            LOGGER.debug("File not found", e);
        } catch (IOException e) {
            status.showMessage(Localization.lang("An Exception occurred while accessing '%0'", url) + "\n\n" + e,
                    getTitle(), JOptionPane.ERROR_MESSAGE);
            LOGGER.debug("Problem accessing URL", e);
        } catch (RuntimeException e) {
            status.showMessage(
                    Localization.lang("An Error occurred while fetching from ADS (%0):", url) + "\n\n" + e.getMessage(),
                    getTitle(), JOptionPane.ERROR_MESSAGE);
            LOGGER.warn("Problem fetching from ADS", e);
        }
        return null;
    }

    private static String constructUrl(String key) {
        return "http://adsabs.harvard.edu/doi/" + key;
    }

    private void importADSAbstract(String key, BibEntry entry, OutputPrinter status) {
        /* TODO: construct ADSUrl from BibEntry */
        String url = constructUrl(key);
        try {
            URL ADSUrl = new URL(url + "&data_type=XML");
            HttpURLConnection ADSConnection = (HttpURLConnection) ADSUrl.openConnection();
            ADSConnection.setRequestProperty("User-Agent", "JabRef");
            BufferedInputStream bis = new BufferedInputStream(ADSConnection.getInputStream());

            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(bis);
            boolean isAbstract = false;
            StringBuilder abstractSB = new StringBuilder();
            while (reader.hasNext()) {
                reader.next();
                if (reader.isStartElement() &&
                        "abstract".equals(reader.getLocalName())) {
                    isAbstract = true;
                }
                if (isAbstract && reader.isCharacters()) {
                    abstractSB.append(reader.getText());
                }
                if (isAbstract && reader.isEndElement()) {
                    isAbstract = false;
                }
            }
            String abstractText = abstractSB.toString();
            abstractText = abstractText.replace("\n", " ");
            entry.setField("abstract", abstractText);
        } catch (XMLStreamException e) {
            status.showMessage(Localization.lang("An Error occurred while parsing abstract"), getTitle(),
                    JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            status.showMessage(Localization.lang("An Exception occurred while accessing '%0'", url) + "\n\n" + e,
                    getTitle(), JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException e) {
            status.showMessage(
                    Localization.lang("An Error occurred while fetching from ADS (%0):", url) + "\n\n" + e.getMessage(),
                    getTitle(), JOptionPane.ERROR_MESSAGE);
        }
    }
}
