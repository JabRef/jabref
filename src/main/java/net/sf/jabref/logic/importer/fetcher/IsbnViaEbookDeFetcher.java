package net.sf.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import net.sf.jabref.logic.formatter.bibtexfields.NormalizeNamesFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.model.cleanup.FieldFormatterCleanup;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.http.client.utils.URIBuilder;

/**
 * Fetcher for ISBN using http://www.ebook.de.
 */
public class IsbnViaEbookDeFetcher extends AbstractIsbnFetcher {

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
        URIBuilder uriBuilder = new URIBuilder("http://www.ebook.de/de/tools/isbn2bibtex");
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
