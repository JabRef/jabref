package org.jabref.logic.importer;

import java.util.Optional;

import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.IsbnFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.ISBN;

public class CompositeIdFetcher {

    private final ImportFormatPreferences importFormatPreferences;
    private final ImporterPreferences importerPreferences;

    public CompositeIdFetcher(ImportFormatPreferences importFormatPreferences, ImporterPreferences importerPreferences) {
        this.importFormatPreferences = importFormatPreferences;
        this.importerPreferences = importerPreferences;
    }

    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        Optional<DOI> doi = DOI.parse(identifier);
        if (doi.isPresent()) {
            return new DoiFetcher(importFormatPreferences).performSearchById(doi.get().getNormalized());
        }
        Optional<ArXivIdentifier> arXivIdentifier = ArXivIdentifier.parse(identifier);
        if (arXivIdentifier.isPresent()) {
            return new ArXivFetcher(importFormatPreferences, importerPreferences).performSearchById(arXivIdentifier.get().getNormalized());
        }
        Optional<ISBN> isbn = ISBN.parse(identifier);
        if (isbn.isPresent()) {
            return new IsbnFetcher(importFormatPreferences).performSearchById(isbn.get().getNormalized());
        }
        /* TODO: IACR is currently disabled, because it needs to be reworked: https://github.com/JabRef/jabref/issues/8876
        Optional<IacrEprint> iacrEprint = IacrEprint.parse(identifier);
        if (iacrEprint.isPresent()) {
            return new IacrEprintFetcher(importFormatPreferences).performSearchById(iacrEprint.get().getNormalized());
        }*/

        return Optional.empty();
    }

    public String getName() {
        return "CompositeIdFetcher";
    }
}
