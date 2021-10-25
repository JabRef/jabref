package org.jabref.logic.importer;

import java.util.Optional;

import org.jabref.logic.importer.fetcher.ArXiv;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.IacrEprintFetcher;
import org.jabref.logic.importer.fetcher.IsbnFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.model.entry.identifier.IacrEprint;

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
            Optional<ArXivIdentifier> arXivIdentifier = ArXivIdentifier.parse(identifier);
            if (arXivIdentifier.isPresent()) {
                return new ArXiv(importFormatPreferences).performSearchById(arXivIdentifier.get().getNormalized());
            }
            Optional<ISBN> isbn = ISBN.parse(identifier);
            if (isbn.isPresent()) {
                return new IsbnFetcher(importFormatPreferences).performSearchById(isbn.get().getNormalized());
            }
            Optional<DOI> doi = DOI.parse(identifier);
            if (doi.isPresent()) {
                return new DoiFetcher(importFormatPreferences).performSearchById(doi.get().getNormalized());
            }
            Optional<IacrEprint> iacrEprint = IacrEprint.parse(identifier);
            if (iacrEprint.isPresent()) {
                return new IacrEprintFetcher(importFormatPreferences).performSearchById(iacrEprint.get().getNormalized());
            }
        } catch (FetcherException fetcherException) {
            LOGGER.error("Error during fetching.", fetcherException);
        }

        return Optional.empty();
    }

    public String getName() {
        return "CompositeIdFetcher";
    }
}
