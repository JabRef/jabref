package net.sf.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.model.entry.BibEntry;

import org.jsoup.helper.StringUtil;

/**
 * Fetcher for ISBN trying ebook.de first and then chimbori.com
 */
public class IsbnFetcher extends AbstractIsbnFetcher {

    public IsbnFetcher(ImportFormatPreferences importFormatPreferences){
        super(importFormatPreferences);
    }

    /**
     * Method never used
     */
    @Override
    public URL getURLForID(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        return null;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        if (StringUtil.isBlank(identifier)) {
            return Optional.empty();
        }

        this.ensureThatIsbnIsValid(identifier);

        IsbnViaEbookDeFetcher isbnViaEbookDeFetcher = new IsbnViaEbookDeFetcher(importFormatPreferences);
        Optional<BibEntry> bibEntry = isbnViaEbookDeFetcher.performSearchById(identifier);
        // nothing found at ebook.de, try chimbori.com
        if (!bibEntry.isPresent()) {
            IsbnViaChimboriFetcher isbnViaChimboriFetcher = new IsbnViaChimboriFetcher(importFormatPreferences);
            bibEntry = isbnViaChimboriFetcher.performSearchById(identifier);
        }

        return bibEntry;
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        // no action needed
    }

}
