package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;

import org.jsoup.helper.StringUtil;

/**
 * Fetcher for ISBN trying ebook.de first and then chimbori.com
 */
public class IsbnFetcher extends AbstractIsbnFetcher {

    public IsbnFetcher(ImportFormatPreferences importFormatPreferences) {
        super(importFormatPreferences);
    }

    @Override
    public String getName() {
        return "ISBN";
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
            LOGGER.debug("No entry found at ebook.de try chimbori.com");
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
