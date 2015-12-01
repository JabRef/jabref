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

import net.sf.jabref.importer.*;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.formatter.bibtexfields.UnitFormatter;
import net.sf.jabref.logic.formatter.casechanger.CaseKeeper;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.util.Util;

public class DiVAtoBibTeXFetcher implements EntryFetcher {

    private static final String URL_PATTERN = "http://www.diva-portal.org/smash/getreferences?referenceFormat=BibTex&pids=%s";
    private final CaseKeeper caseKeeper = new CaseKeeper();
    private final UnitFormatter unitFormatter = new UnitFormatter();
    private final HTMLConverter htmlConverter = new HTMLConverter();


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

        String urlString = String.format(DiVAtoBibTeXFetcher.URL_PATTERN, q);

        // Send the request
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        }

        String bibtexString;
        try {
            bibtexString = Util.getResultsWithEncoding(url, StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            status.showMessage(Localization.lang("Unknown DiVA entry: '%0'.",
                            query),
                    Localization.lang("Get BibTeX entry from DiVA"), JOptionPane.INFORMATION_MESSAGE);
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
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

            String institution = entry.getField("institution");
            if (institution != null) {
                institution = htmlConverter.formatUnicode(institution);
                entry.setField("institution", institution);
            }
            // Do not use the provided key
            // entry.setField(BibtexFields.KEY_FIELD,null);
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
    public String getHelpPage() {
        return "DiVAtoBibTeXHelp.html";
    }

    @Override
    public JPanel getOptionsPanel() {
        // no additional options available
        return null;
    }

}
