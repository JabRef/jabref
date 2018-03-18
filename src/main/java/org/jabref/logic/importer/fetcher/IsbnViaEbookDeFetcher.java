package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import org.apache.http.client.utils.URIBuilder;

/**
 * Fetcher for ISBN using http://www.ebook.de.
 */
public class IsbnViaEbookDeFetcher extends AbstractIsbnFetcher {
    private static final String BASE_URL = "http://www.ebook.de/de/tools/isbn2bibtex";

    public IsbnViaEbookDeFetcher(ImportFormatPreferences importFormatPreferences) {
        super(importFormatPreferences);
    }

    @Override
    public String getName() {
        return "ISBN (ebook.de)";
    }

    @Override
    public URL getURLForID(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        this.ensureThatIsbnIsValid(identifier);
        URIBuilder uriBuilder = new URIBuilder(BASE_URL);
        uriBuilder.addParameter("isbn", identifier);
        return uriBuilder.build().toURL();
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        // We MUST NOT clean the URL. this is the deal with ebook.de
        // DO NOT add following code:
        // new FieldFormatterCleanup(FieldName.URL, new ClearFormatter()).cleanup(entry);

        // Fetcher returns page numbers as "30 Seiten" -> remove every non-digit character in the PAGETOTAL field
        entry.getField(FieldName.PAGETOTAL).ifPresent(pages ->
                entry.setField(FieldName.PAGETOTAL, pages.replaceAll("[\\D]", "")));
        new FieldFormatterCleanup(FieldName.PAGETOTAL, new NormalizePagesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(FieldName.AUTHOR, new NormalizeNamesFormatter()).cleanup(entry);
    }

}
