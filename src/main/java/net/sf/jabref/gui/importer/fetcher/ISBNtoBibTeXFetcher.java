package net.sf.jabref.gui.importer.fetcher;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Scanner;

import javax.swing.JPanel;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import net.sf.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.ImportInspector;
import net.sf.jabref.logic.importer.OutputPrinter;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class uses ebook.de's ISBN to BibTeX Converter to convert an ISBN to a BibTeX entry <br />
 * There is no separate web-based converter available, just that API
 */
public class ISBNtoBibTeXFetcher implements EntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(ISBNtoBibTeXFetcher.class);

    private static final String URL_PATTERN = "http://www.ebook.de/de/tools/isbn2bibtex?isbn=%s";
    private final ProtectTermsFormatter protectTermsFormatter = new ProtectTermsFormatter();
    private final UnitsToLatexFormatter unitsToLatexFormatter = new UnitsToLatexFormatter();


    @Override
    public void stopFetching() {
        // nothing needed as the fetching is a single HTTP GET
    }

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        Optional<BibEntry> entry = getEntryFromISBN(query, status);
        if (entry.isPresent()) {
            inspector.addEntry(entry.get());
            return true;
        }
        return false;

    }

    public Optional<BibEntry> getEntryFromISBN(String query, OutputPrinter status) {
        String q;
        try {
            q = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // this should never happen
            if (status != null) {
                status.setStatus(Localization.lang("Error"));
            }
            LOGGER.warn("Shouldn't happen...", e);
            return Optional.empty();
        }

        String urlString = String.format(ISBNtoBibTeXFetcher.URL_PATTERN, q);

        // Send the request
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            LOGGER.warn("Bad URL when fetching ISBN info", e);
            return Optional.empty();
        }

        try(InputStream source = url.openStream()) {
            String bibtexString;
            try(Scanner scan = new Scanner(source)) {
                bibtexString = scan.useDelimiter("\\A").next();
            }

            Optional<BibEntry> bibEntry = BibtexParser.singleFromString(bibtexString,
                    ImportFormatPreferences.fromPreferences(Globals.prefs));
            bibEntry.ifPresent(entry -> {
                // Remove the added " Seiten" from the "pagetotal" field
                entry.getFieldOptional(FieldName.PAGETOTAL)
                        .ifPresent(pagetotal -> entry.setField(FieldName.PAGETOTAL, pagetotal.replace(" Seiten", "")));

                // Optionally add curly brackets around key words to keep the case
                entry.getFieldOptional(FieldName.TITLE).ifPresent(title -> {
                    // Unit formatting
                    if (Globals.prefs.getBoolean(JabRefPreferences.USE_UNIT_FORMATTER_ON_SEARCH)) {
                        title = unitsToLatexFormatter.format(title);
                    }

                    // Case keeping
                    if (Globals.prefs.getBoolean(JabRefPreferences.USE_CASE_KEEPER_ON_SEARCH)) {
                        title = protectTermsFormatter.format(title);
                    }
                    entry.setField(FieldName.TITLE, title);
                });
            });
            return bibEntry;
        } catch (FileNotFoundException e) {
            // invalid ISBN --> 404--> FileNotFoundException
            if (status != null) {
                status.showMessage(Localization.lang("No entry found for ISBN %0 at www.ebook.de", query));
            }
            LOGGER.debug("No ISBN info found", e);
            return Optional.empty();
        } catch (UnknownHostException e) {
            // It is very unlikely that ebook.de is an unknown host
            // It is more likely that we don't have an internet connection
            if (status != null) {
                status.showMessage(Localization.lang("No_internet_connection."));
            }
            LOGGER.debug("No internet connection", e);
            return Optional.empty();
        } catch (Exception e) {
            if (status != null) {
                status.showMessage(e.toString());
            }
            LOGGER.warn("Problem getting info for ISBN", e);
            return Optional.empty();
        }
    }

    @Override
    public String getTitle() {
        return "ISBN to BibTeX";

    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_ISBN_TO_BIBTEX;
    }

    @Override
    public JPanel getOptionsPanel() {
        // no additional options available
        return null;
    }

}
