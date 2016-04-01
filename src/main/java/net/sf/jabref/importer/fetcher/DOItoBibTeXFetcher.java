/*  Copyright (C) 2014 JabRef contributors.
    Copyright (C) 2015 Oliver Kopp

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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.importer.*;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.logic.util.DOI;

public class DOItoBibTeXFetcher implements EntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(DOItoBibTeXFetcher.class);

    private final ProtectTermsFormatter protectTermsFormatter = new ProtectTermsFormatter();
    private final UnitsToLatexFormatter unitsToLatexFormatter = new UnitsToLatexFormatter();


    @Override
    public void stopFetching() {
        // nothing needed as the fetching is a single HTTP GET
    }

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        BibEntry entry = getEntryFromDOI(query, status);
        if (entry != null) {
            inspector.addEntry(entry);
            return true;
        }
        return false;
    }

    @Override
    public String getTitle() {
        return "DOI to BibTeX";
    }

    @Override
    public String getHelpPage() {
        return "DOItoBibTeXHelp";
    }

    @Override
    public JPanel getOptionsPanel() {
        // no additional options available
        return null;
    }

    public BibEntry getEntryFromDOI(String doiStr, OutputPrinter status) {
        DOI doi;
        try {
            doi = new DOI(doiStr);
        } catch (IllegalArgumentException e) {
            status.showMessage(Localization.lang("Invalid DOI: '%0'.", doiStr),
                    Localization.lang("Get BibTeX entry from DOI"),
                    JOptionPane.INFORMATION_MESSAGE);
            LOGGER.warn("Invalid DOI", e);
            return null;
        }

        // Send the request

        // construct URL
        URL url;
        try {
            Optional<URI> uri = doi.getURI();
            if (uri.isPresent()) {
                url = uri.get().toURL();
            } else {
                return null;
            }
        } catch (MalformedURLException e) {
            LOGGER.warn("Bad URL", e);
            return null;
        }

        String bibtexString = "";
        try {
            URLDownload dl = new URLDownload(url);
            dl.addParameters("Accept", "application/x-bibtex");
            bibtexString = dl.downloadToString(StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            if (status != null) {
                status.showMessage(Localization.lang("Unknown DOI: '%0'.", doi.getDOI()),
                        Localization.lang("Get BibTeX entry from DOI"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
            LOGGER.debug("Unknown DOI", e);
            return null;
        } catch (IOException e) {
            LOGGER.warn("Communication problems", e);
            return null;
        }

        //Usually includes an en-dash in the page range. Char is in cp1252 but not
        // ISO 8859-1 (which is what latex expects). For convenience replace here.
        bibtexString = bibtexString.replaceAll("(pages=\\{[0-9]+)\u2013([0-9]+\\})", "$1--$2");
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
        }
        return entry;
    }
}
