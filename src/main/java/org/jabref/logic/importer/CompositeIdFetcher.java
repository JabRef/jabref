package org.jabref.logic.importer;

import java.util.Optional;

import org.jabref.logic.importer.fetcher.ArXiv;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.IacrEprintFetcher;
import org.jabref.logic.importer.fetcher.IsbnFetcher;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositeIdFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeIdFetcher.class);

    private final ImportFormatPreferences importFormatPreferences;

    public CompositeIdFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    public Optional<BibEntry> performSearchById(String identifier) {

        Optional<BibEntry> fetchedEntry;

        try {
            ArXiv arXiv = new ArXiv(importFormatPreferences);
            fetchedEntry = arXiv.performSearchById(identifier);
            if (fetchedEntry.isPresent()) {
                return fetchedEntry;
            }
        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        try {
            IsbnFetcher isbnFetcher = new IsbnFetcher(importFormatPreferences);
            fetchedEntry = isbnFetcher.performSearchById(identifier);
            if (fetchedEntry.isPresent()) {
                return fetchedEntry;
            }
        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        try {
            DoiFetcher doiFetcher = new DoiFetcher(importFormatPreferences);
            fetchedEntry = doiFetcher.performSearchById(identifier);
            if (fetchedEntry.isPresent()) {
                return fetchedEntry;
            }
        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        try {
            IacrEprintFetcher iacrEprintFetcher = new IacrEprintFetcher(importFormatPreferences);
            fetchedEntry = iacrEprintFetcher.performSearchById(identifier);
            if (fetchedEntry.isPresent()) {
                return fetchedEntry;
            }
        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        try {
            CrossRef crossRef = new CrossRef();
            fetchedEntry = crossRef.performSearchById(identifier);
            if (fetchedEntry.isPresent()) {
                return fetchedEntry;
            }
        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        return Optional.empty();

    }

    public String getName() {
        return "CompositeIdFetcher";
    }
}
