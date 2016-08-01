package net.sf.jabref.logic.importer.fetcher;


import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Scanner;

import net.sf.jabref.Globals;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import net.sf.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.util.ISBN;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IsbnFetcher implements IdBasedFetcher {

    private static final Log LOGGER = LogFactory.getLog(IsbnFetcher.class);

    private static final String URL_PATTERN = "http://www.ebook.de/de/tools/isbn2bibtex?isbn=%s";
    private final ProtectTermsFormatter protectTermsFormatter = new ProtectTermsFormatter();
    private final UnitsToLatexFormatter unitsToLatexFormatter = new UnitsToLatexFormatter();

    @Override
    public String getName() {
        return "ISBN";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_ISBN_TO_BIBTEX;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        ISBN isbn = new ISBN(identifier);
        String q = "";
        Optional<BibEntry> result;
        BibEntry entry = null;

        if (isbn.isValidChecksum() && isbn.isValidFormat()) {
            try {
                q = URLEncoder.encode(identifier, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                LOGGER.warn("Shouldn't happen...", e);
            }

            String urlString = String.format(URL_PATTERN, q);

            // Send the request
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                LOGGER.warn("Bad URL when fetching ISBN info", e);
            }

            try (InputStream source = url.openStream()) {
                String bibtexString;
                try (Scanner scan = new Scanner(source)) {
                    bibtexString = scan.useDelimiter("\\A").next();
                }

                entry = BibtexParser.singleFromString(bibtexString);
                if (entry != null) {
                    // Optionally add curly brackets around key words to keep the case
                    String title = entry.getField("title");
                    if (title != null) {
                        // Unit formatting
                        if (Globals.prefs.getBoolean(JabRefPreferences.USE_UNIT_FORMATTER_ON_SEARCH)) {
                            title = unitsToLatexFormatter.format(title);
                        }

                        // Case keeping
                        if (Globals.prefs.getBoolean(JabRefPreferences.USE_CASE_KEEPER_ON_SEARCH)) {
                            title = protectTermsFormatter.format(title);
                        }
                        entry.setField("title", title);
                    }
                    result = Optional.ofNullable(entry);
                    return result;
                }
            } catch (IOException e) {
                LOGGER.warn("Error opening URL");
            }
        }
        return Optional.ofNullable(entry);
    }
}
