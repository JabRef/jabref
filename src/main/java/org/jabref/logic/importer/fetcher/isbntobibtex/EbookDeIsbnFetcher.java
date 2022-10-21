package org.jabref.logic.importer.fetcher.isbntobibtex;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fetcher.AbstractIsbnFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.apache.http.client.utils.URIBuilder;

/**
 * Fetcher for ISBN using <a href="https://www.ebook.de">https://www.ebook.de</a>.
 */
public class EbookDeIsbnFetcher extends AbstractIsbnFetcher {
    private static final String BASE_URL = "https://www.ebook.de/de/tools/isbn2bibtex";

    public EbookDeIsbnFetcher(ImportFormatPreferences importFormatPreferences) {
        super(importFormatPreferences);
    }

    @Override
    public String getName() {
        return "ISBN (ebook.de)";
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        this.ensureThatIsbnIsValid(identifier);
        return new URIBuilder(BASE_URL)
                .addParameter("isbn", identifier)
                .build()
                .toURL();
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        // We MUST NOT clean the URL. this is the deal with ebook.de
        // DO NOT add following code:
        // new FieldFormatterCleanup(StandardField.URL, new ClearFormatter()).cleanup(entry);

        // Fetcher returns page numbers as "30 Seiten" -> remove every non-digit character in the PAGETOTAL field
        entry.getField(StandardField.PAGETOTAL).ifPresent(pages ->
                entry.setField(StandardField.PAGETOTAL, pages.replaceAll("[\\D]", "")));
        new FieldFormatterCleanup(StandardField.PAGETOTAL, new NormalizePagesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(StandardField.AUTHOR, new NormalizeNamesFormatter()).cleanup(entry);
    }
}
