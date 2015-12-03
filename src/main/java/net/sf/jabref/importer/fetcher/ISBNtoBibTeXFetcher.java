/*  Copyright (C) 2012, 2015 JabRef contributors.
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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import javax.swing.JPanel;

import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.logic.formatter.bibtexfields.UnitFormatter;
import net.sf.jabref.logic.formatter.casechanger.CaseKeeper;
import net.sf.jabref.logic.l10n.Localization;

/**
 * This class uses ebook.de's ISBN to BibTeX Converter to convert an ISBN to a BibTeX entry <br />
 * There is no separate web-based converter available, just that API
 */
public class ISBNtoBibTeXFetcher implements EntryFetcher {

    private static final String URL_PATTERN = "http://www.ebook.de/de/tools/isbn2bibtex?isbn=%s";
    private final CaseKeeper caseKeeper = new CaseKeeper();
    private final UnitFormatter unitFormatter = new UnitFormatter();


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
            e.printStackTrace();
            return false;
        }

        String urlString = String.format(ISBNtoBibTeXFetcher.URL_PATTERN, q);

        // Send the request
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }

        try(InputStream source = url.openStream()) {
            String bibtexString;
            try(Scanner scan = new Scanner(source)) {
                bibtexString = scan.useDelimiter("\\A").next();
            }

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

                inspector.addEntry(entry);
                return true;
            }
            return false;
        } catch (FileNotFoundException e) {
            // invalid ISBN --> 404--> FileNotFoundException
            status.showMessage(Localization.lang("Invalid ISBN"));
            return false;
        } catch (java.net.UnknownHostException e) {
            // It is very unlikely that ebook.de is an unknown host
            // It is more likely that we don't have an internet connection
            status.showMessage(Localization.lang("No_Internet_Connection."));
            return false;
        } catch (Exception e) {
            status.showMessage(e.toString());
            return false;
        }

    }

    @Override
    public String getTitle() {
        return "ISBN to BibTeX";
    }

    @Override
    public String getHelpPage() {
        return "ISBNtoBibTeXHelp.html";
    }

    @Override
    public JPanel getOptionsPanel() {
        // no additional options available
        return null;
    }

}
