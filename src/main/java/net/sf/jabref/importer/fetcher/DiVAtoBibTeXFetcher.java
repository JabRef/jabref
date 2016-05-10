/*  Copyright (C) 2012 JabRef contributors.
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sf.jabref.importer.fetcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.formatter.bibtexfields.UnicodeToLatexFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import net.sf.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DiVAtoBibTeXFetcher implements EntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(DiVAtoBibTeXFetcher.class);

    private static final String URL_PATTERN = "http://www.diva-portal.org/smash/getreferences?referenceFormat=BibTex&pids=%s";
    private final ProtectTermsFormatter protectTermsFormatter = new ProtectTermsFormatter();
    private final UnitsToLatexFormatter unitsToLatexFormatter = new UnitsToLatexFormatter();

    @Override
    public void stopFetching() {
        // nothing needed as the fetching is a single HTTP GET
    }

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        String q;
        try {
            q = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // this should never happen
            status.setStatus(Localization.lang("Error"));
            LOGGER.warn("Encoding issues", e);
            return false;
        }

        String urlString = String.format(DiVAtoBibTeXFetcher.URL_PATTERN, q);

        // Send the request
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            LOGGER.warn("Bad URL", e);
            return false;
        }

        String bibtexString;
        try {
            URLDownload dl = new URLDownload(url);

            bibtexString = dl.downloadToString(StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            status.showMessage(Localization.lang("Unknown DiVA entry: '%0'.",
                            query),
                    Localization.lang("Get BibTeX entry from DiVA"), JOptionPane.INFORMATION_MESSAGE);
            return false;
        } catch (IOException e) {
            LOGGER.warn("Communication problems", e);
            return false;
        }

        BibEntry entry = BibtexParser.singleFromString(bibtexString);
        if (entry != null) {
            // Optionally add curly brackets around key words to keep the case
            entry.getFieldOptional("title").ifPresent(title -> {
                // Unit formatting
                if (Globals.prefs.getBoolean(JabRefPreferences.USE_UNIT_FORMATTER_ON_SEARCH)) {
                    title = unitsToLatexFormatter.format(title);
                }

                // Case keeping
                if (Globals.prefs.getBoolean(JabRefPreferences.USE_CASE_KEEPER_ON_SEARCH)) {
                    title = protectTermsFormatter.format(title);
                }
                entry.setField("title", title);
            });

            entry.getFieldOptional("institution").ifPresent(
                    institution -> entry.setField("institution", new UnicodeToLatexFormatter().format(institution)));
            // Do not use the provided key
            // entry.clearField(InternalBibtexFields.KEY_FIELD);
            inspector.addEntry(entry);

            return true;
        }
        return false;
    }

    @Override
    public String getTitle() {
        return "DiVA";
    }

    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_DIVA_TO_BIBTEX;
    }

    @Override
    public JPanel getOptionsPanel() {
        // no additional options available
        return null;
    }

}
