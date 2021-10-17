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

public class CompositeIdFetcher {

    private final ImportFormatPreferences importFormatPreferences;

    public CompositeIdFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    public Optional<BibEntry> performSearchById(String identifier) {
        try {
            if (ArXivIdentifier.parse(identifier).isPresent()) {
                return new ArXiv(importFormatPreferences).performSearchById(identifier);
            } else if (ISBN.parse(identifier).isPresent()) {
                return new IsbnFetcher(importFormatPreferences).performSearchById(identifier);
            } else if (DOI.parse(identifier).isPresent()) {
                return new DoiFetcher(importFormatPreferences).performSearchById(identifier);
            } else if (IacrEprint.parse(identifier).isPresent()) {
                return new IacrEprintFetcher(importFormatPreferences).performSearchById(identifier);
            }
        } catch (FetcherException fetcherException) {
            fetcherException.printStackTrace();
        }

        return Optional.empty();

    }

    public String getName() {
        return "CompositeIdFetcher";
    }
}
