package org.jabref.logic.importer;

import java.util.Optional;

import org.jabref.logic.importer.fetcher.ArXiv;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.IacrEprintFetcher;
import org.jabref.logic.importer.fetcher.IsbnFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.Eprint;
import org.jabref.model.entry.identifier.ISBN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositeIdFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeIdFetcher.class);

    private final ImportFormatPreferences importFormatPreferences;

    public CompositeIdFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    public Optional<BibEntry> performSearchById(String identifier) {
        try {
            if (ArXivIdentifier.parse(identifier).isPresent()) {
                return new ArXiv(importFormatPreferences).performSearchById(identifier);
            }
        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        try {
            if (ISBN.parse(identifier).isPresent()) {
                return new IsbnFetcher(importFormatPreferences).performSearchById(identifier);
            }
        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        try {
            if (DOI.parse(identifier).isPresent()) {
                return new DoiFetcher(importFormatPreferences).performSearchById(identifier);
            }
        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        try {
            if (DOI.parse(identifier).isPresent()) {
                return new CrossRef().performSearchById(identifier);
            }
        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        try {
            if (Eprint.build(identifier).isPresent()) {
                return new IacrEprintFetcher(importFormatPreferences).performSearchById(identifier);
            }
        } catch (FetcherException fetcherException) {
            LOGGER.debug(fetcherException.getMessage());
        }

        return Optional.empty();

    }

}
