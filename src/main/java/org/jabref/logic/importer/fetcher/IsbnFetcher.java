package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.OptionalUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetcher for ISBN trying ebook.de first, chimbori.com and then ottobib
 */
public class IsbnFetcher implements EntryBasedFetcher, IdBasedFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(IsbnFetcher.class);
    protected final ImportFormatPreferences importFormatPreferences;

    public IsbnFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public String getName() {
        return "ISBN";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_ISBN);
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        if (StringUtil.isBlank(identifier)) {
            return Optional.empty();
        }

        IsbnViaEbookDeFetcher isbnViaEbookDeFetcher = new IsbnViaEbookDeFetcher(importFormatPreferences);
        Optional<BibEntry> bibEntry = isbnViaEbookDeFetcher.performSearchById(identifier);
        
        // nothing found at ebook.de, try chimbori.com
        if (!bibEntry.isPresent()) {
            LOGGER.debug("No entry found at ebook.de try chimbori.com");
            IsbnViaChimboriFetcher isbnViaChimboriFetcher = new IsbnViaChimboriFetcher(importFormatPreferences);
            bibEntry = isbnViaChimboriFetcher.performSearchById(identifier);
        }

        //nothing found at ebook.de and chimbori.com, try ottobib
        if (!bibEntry.isPresent()) {
            LOGGER.debug("No entry found at ebook.de and chimbori.com try ottobib");
            IsbnViaOttoBibFetcher isbnViaOttoBibFetcher = new IsbnViaOttoBibFetcher(importFormatPreferences);
            bibEntry = isbnViaOttoBibFetcher.performSearchById(identifier);
        }

        return bibEntry;
    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        Optional<String> isbn = entry.getField(FieldName.ISBN);
        if (isbn.isPresent()) {
            return OptionalUtil.toList(performSearchById(isbn.get()));
        } else {
            return Collections.emptyList();
        }
    }
}
