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
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.jabref.importer.*;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.formatter.bibtexfields.UnitFormatter;
import net.sf.jabref.logic.formatter.casechanger.CaseKeeper;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.util.Util;

public class DOItoBibTeXFetcher implements EntryFetcher {

    private final CaseKeeper caseKeeper = new CaseKeeper();
    private final UnitFormatter unitFormatter = new UnitFormatter();


    @Override
    public void stopFetching() {
        // nothing needed as the fetching is a single HTTP GET
    }

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        BibtexEntry entry = getEntryFromDOI(query, status);
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
        return "DOItoBibTeXHelp.html";
    }

    @Override
    public JPanel getOptionsPanel() {
        // no additional options available
        return null;
    }

    public BibtexEntry getEntryFromDOI(String doiStr, OutputPrinter status) {
        DOI doi;
        try {
            doi = new DOI(doiStr);
        } catch (IllegalArgumentException e) {
            status.showMessage(Localization.lang("Invalid DOI: '%0'.", doiStr),
                    Localization.lang("Get BibTeX entry from DOI"),
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        // Send the request

        // construct URL
        URL url;
        try {
            url = doi.getURI().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }

        URLConnection conn;
        try {
            conn = url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        conn.setRequestProperty("Accept", "application/x-bibtex");

        String bibtexString;
        try {
            bibtexString = Util.getResultsWithEncoding(conn, StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {

            if (status != null) {
                // @formatter:off
                status.showMessage(Localization.lang("Unknown DOI: '%0'.", doi.getDOI()),
                        Localization.lang("Get BibTeX entry from DOI"),
                        JOptionPane.INFORMATION_MESSAGE);
                // @formatter:on
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        //Usually includes an en-dash in the page range. Char is in cp1252 but not
        // ISO 8859-1 (which is what latex expects). For convenience replace here.
        bibtexString = bibtexString.replaceAll("(pages=\\{[0-9]+)\u2013([0-9]+\\})", "$1--$2");
        BibtexEntry entry = BibtexParser.singleFromString(bibtexString);

        if (entry != null) {
            // Optionally add curly brackets around key words to keep the case
            String title = entry.getField("title");
            if (title != null) {

                // Unit formatting
                if (Globals.prefs.getBoolean(JabRefPreferences.USE_UNIT_FORMATTER_ON_SEARCH)) {
                    title = unitFormatter.format(title);
                }

                // Case keeping
                if (Globals.prefs.getBoolean(JabRefPreferences.USE_CASE_KEEPER_ON_SEARCH)) {
                    title = caseKeeper.format(title);
                }
                entry.setField("title", title);
            }
        }
        return entry;
    }
}
