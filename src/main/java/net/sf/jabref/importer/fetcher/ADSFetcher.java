/* Copyright (c) 2009, Ryo IGARASHI <rigarash@gmail.com>
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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.jabref.importer.*;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.logic.l10n.Localization;

/**
 *
 * This class handles accessing and obtaining BibTeX entry
 * from ADS(The NASA Astrophysics Data System).
 * Fetching using DOI(Document Object Identifier) is only supported.
 *
 * @author Ryo IGARASHI
 * @version $Id$
 */
public class ADSFetcher implements EntryFetcher {

    @Override
    public JPanel getOptionsPanel() {
        // No option panel
        return null;
    }

    @Override
    public String getHelpPage() {
        // TODO: No help page
        return null;
    }

    @Override
    public String getKeyName() {
        return "ADS from ADS-DOI";
    }

    @Override
    public String getTitle() {
        return Localization.menuTitle(getKeyName());
    }

    @Override
    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter status) {
        try {
            /* Remove "doi:" scheme identifier */
            query = query.replaceAll("^(doi:|DOI:)", "");
            /* Allow fetching only 1 key */
            String key = query;
            /* Query ADS and load the results into the BibtexDatabase */
            status.setStatus(Localization.lang("Processing ") + key);
            BibtexDatabase bd = importADSEntries(key, status);
            /* Add the entry to the inspection dialog */
            status.setStatus("Adding fetched entries");
            if (bd.getEntryCount() > 0) {
                for (BibtexEntry entry : bd.getEntries()) {
                    importADSAbstract(key, entry, status);
                    dialog.addEntry(entry);
                }
            }
        } catch (Exception e) {
            status.setStatus(Localization.lang("Error while fetching from ADS") + ": " + e.getMessage());
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void stopFetching() {
    }

    private BibtexDatabase importADSEntries(String key, OutputPrinter status) {
        String url = constructUrl(key);
        try {
            URL ADSUrl = new URL(url + "&data_type=BIBTEX");
            HttpURLConnection ADSConnection = (HttpURLConnection) ADSUrl.openConnection();
            ADSConnection.setRequestProperty("User-Agent", "Jabref");
            InputStream is = ADSConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            ParserResult pr = BibtexParser.parse(reader);
            return pr.getDatabase();
        } catch (IOException e) {
            status.showMessage(Localization.lang(
                    "An Exception ocurred while accessing '%0'", url)
                    + "\n\n" + e, Localization.lang(getKeyName()), JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException e) {
            status.showMessage(Localization.lang(
                    "An Error occurred while fetching from ADS (%0):", new String[]{url})
                    + "\n\n" + e.getMessage(), Localization.lang(getKeyName()), JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private String constructUrl(String key) {
        return "http://adsabs.harvard.edu/doi/" + key;
    }

    private void importADSAbstract(String key, BibtexEntry entry, OutputPrinter status) {
        /* TODO: construct ADSUrl from BibtexEntry */
        String url = constructUrl(key);
        try {
            URL ADSUrl = new URL(url + "&data_type=XML");
            HttpURLConnection ADSConnection = (HttpURLConnection) ADSUrl.openConnection();
            ADSConnection.setRequestProperty("User-Agent", "Jabref");
            BufferedInputStream bis = new BufferedInputStream(ADSConnection.getInputStream());

            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(bis);
            boolean isAbstract = false;
            String abstractText = "";
            while (reader.hasNext()) {
                reader.next();
                if (reader.isStartElement() &&
                        reader.getLocalName().equals("abstract")) {
                    isAbstract = true;
                }
                if (isAbstract && reader.isCharacters()) {
                    abstractText = abstractText + reader.getText();
                }
                if (isAbstract && reader.isEndElement()) {
                    isAbstract = false;
                }
            }
            abstractText = abstractText.replace("\n", " ");
            entry.setField("abstract", abstractText);
        } catch (XMLStreamException e) {
            status.showMessage(Localization.lang(
                            "An Error occurred while parsing abstract"),
                    Localization.lang(getKeyName()), JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            status.showMessage(Localization.lang(
                    "An Exception ocurred while accessing '%0'", url)
                    + "\n\n" + e, Localization.lang(getKeyName()), JOptionPane.ERROR_MESSAGE);
        } catch (RuntimeException e) {
            status.showMessage(Localization.lang(
                    "An Error occurred while fetching from ADS (%0):", new String[]{url})
                    + "\n\n" + e.getMessage(), Localization.lang(getKeyName()), JOptionPane.ERROR_MESSAGE);
        }
    }
}
