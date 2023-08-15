package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.IdFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.integrity.ISSNChecker;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.ISSN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetcher to generate the BibTex entry from an ISSN.
 * The idea is to use the {@link DOAJFetcher} to do a request for a given ISSN number.
 */

public class IssnFetcher implements IdBasedFetcher, IdFetcher<ISSN> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IssnFetcher.class);
    private final DOAJFetcher doajFetcher;
    private final String SEARCH_URL = "https://doaj.org/api/search/journals/";
    private final ISSNChecker issnChecker;
    private final ImportFormatPreferences importFormatPreferences;

    public IssnFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
        this.doajFetcher = new DOAJFetcher(importFormatPreferences);
        this.issnChecker = new ISSNChecker();
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {

        Optional<String> checkedId = issnChecker.checkValue(identifier);
        if (checkedId.isEmpty()) {
            LOGGER.warn("Not a valid ISSN");
            return Optional.empty();
        }

        Optional<BibEntry> bibEntry = Optional.empty();

        String queryString = concatenateIssnWithId(identifier);
        List<BibEntry> bibEntries = doajFetcher.performSearch(queryString);

        if (!bibEntries.isEmpty()) {
            for (int i = 0; i < bibEntries.size(); i++) {
                bibEntry = Optional.ofNullable(bibEntries.get(0));
            }
        } else {
            LOGGER.warn("ISSN search failed, no results found");
        }

        return bibEntry;
    }

    @Override
    public Optional<ISSN> findIdentifier(BibEntry entry) throws FetcherException {
        // Need to create a getIssn() method in BibEntry that returns a Optional
        return Optional.empty();
    }

    @Override
    public String getIdentifierName() {
        return getName();
    }

    @Override
    public String getName() {
        return "ISSN";
    }

    public String concatenateIssnWithId(String identifier) {
        return "issn:" + identifier;
    }
}
