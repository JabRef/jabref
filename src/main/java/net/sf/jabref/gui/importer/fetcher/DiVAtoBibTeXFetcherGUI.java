package net.sf.jabref.gui.importer.fetcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.swing.JOptionPane;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fetcher.DiVAtoBibTeXFetcher;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.formatter.bibtexfields.UnicodeToLatexFormatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.model.entry.BibEntry;

public class DiVAtoBibTeXFetcherGUI extends DiVAtoBibTeXFetcher implements EntryFetcherGUI {

    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_DIVA_TO_BIBTEX;
    }

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        String q;
        try {
            q = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // this should never happen
            status.setStatus(Localization.lang("Error"));
            getLogger().warn("Encoding issues", e);
            return false;
        }

        String urlString = String.format(getUrlPattern(), q);

        // Send the request
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            getLogger().warn("Bad URL", e);
            return false;
        }

        String bibtexString;
        try {
            URLDownload dl = new URLDownload(url);

            bibtexString = dl.downloadToString(StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            status.showMessage(Localization.lang("Unknown DiVA entry: '%0'.", query),
                    Localization.lang("Get BibTeX entry from DiVA"), JOptionPane.INFORMATION_MESSAGE);
            return false;
        } catch (IOException e) {
            getLogger().warn("Communication problems", e);
            return false;
        }

        BibEntry entry = BibtexParser.singleFromString(bibtexString);
        if (entry != null) {
            // Optionally add curly brackets around key words to keep the case
            entry.getFieldOptional("title").ifPresent(title -> {
                // Unit formatting
                if (Globals.prefs.getBoolean(JabRefPreferences.USE_UNIT_FORMATTER_ON_SEARCH)) {
                    title = getUnitsToLatexFormatter().format(title);
                }

                // Case keeping
                if (Globals.prefs.getBoolean(JabRefPreferences.USE_CASE_KEEPER_ON_SEARCH)) {
                    title = getProtectTermsFormatter().format(title);
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
}
