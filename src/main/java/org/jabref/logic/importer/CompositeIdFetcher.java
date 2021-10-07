package org.jabref.logic.importer;

import org.jabref.logic.importer.fetcher.*;
import org.jabref.model.entry.BibEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

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
            if ((fetchedEntry = new ArXiv(importFormatPreferences).performSearchById(identifier)).isPresent()) {
                return fetchedEntry;
            }

        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        try {
            if ((fetchedEntry = new IsbnFetcher(importFormatPreferences).performSearchById(identifier)).isPresent()) {
                return fetchedEntry;
            }

        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        try {
            if ((fetchedEntry = new DoiFetcher(importFormatPreferences).performSearchById(identifier)).isPresent()) {
                return fetchedEntry;
            }

        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        try {
            if ((fetchedEntry = new IacrEprintFetcher(importFormatPreferences).performSearchById(identifier)).isPresent()) {
                return fetchedEntry;
            }

        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        try {
            if ((fetchedEntry = new CrossRef().performSearchById(identifier)).isPresent()) {
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
