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

public class CompositeIdFetcher implements IdBasedFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeIdFetcher.class);

    private final ImportFormatPreferences importFormatPreferences;

    public CompositeIdFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) {

        Optional<BibEntry> fetchedEntry;

        try {
            fetchedEntry = new ArXiv(importFormatPreferences).performSearchById(identifier);
            if (fetchedEntry.isPresent()) {
                return fetchedEntry;
            }
        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        try {
            fetchedEntry = new IsbnFetcher(importFormatPreferences).performSearchById(identifier);
            if (fetchedEntry.isPresent()) {
                return fetchedEntry;
            }
        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        try {
            fetchedEntry = new DoiFetcher(importFormatPreferences).performSearchById(identifier);
            if (fetchedEntry.isPresent()) {
                return fetchedEntry;
            }
        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        try {
            fetchedEntry = new IacrEprintFetcher(importFormatPreferences).performSearchById(identifier);
            if (fetchedEntry.isPresent()) {
                return fetchedEntry;
            }
        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        try {
            fetchedEntry = new CrossRef().performSearchById(identifier);
            if (fetchedEntry.isPresent()) {
                return fetchedEntry;
            }
        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        return Optional.empty();

    }

    @Override
    public String getName() {
        return "CompositeIdFetcher";
    }
}
