package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.IdFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.ISSN;
import org.jabref.model.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetcher to generate the BibTex entry from an ISSN.
 * The idea is to use the {@link DOAJFetcher} to do a request for a given ISSN number.
 */

public class IssnFetcher implements IdBasedFetcher, IdFetcher<ISSN> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IssnFetcher.class);
    private final DOAJFetcher doajFetcher;
    private final ImportFormatPreferences importFormatPreferences;

    public IssnFetcher(ImportFormatPreferences importFormatPreferences) {
        this.doajFetcher = new DOAJFetcher(importFormatPreferences);
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {

        if (StringUtil.isBlank(identifier)) {
            return Optional.empty();
        }

        Optional<BibEntry> bibEntry = Optional.empty();

        // need to create a queryString for ISSN
        // take a look on arXiv API manual
        List<BibEntry> bibEntries = doajFetcher.performSearch(identifier);
        for (BibEntry entry: bibEntries) {
            bibEntry = Optional.ofNullable(bibEntries.get(0));
        }

        return bibEntry;
    }

    @Override
    public Optional<ISSN> findIdentifier(BibEntry entry) throws FetcherException {
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
}
