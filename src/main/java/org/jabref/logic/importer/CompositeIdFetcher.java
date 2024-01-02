package org.jabref.logic.importer;

import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.isbntobibtex.IsbnFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.ISBN;

public class CompositeIdFetcher {

    private final ImportFormatPreferences importFormatPreferences;

    public CompositeIdFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        Optional<DOI> doi = DOI.findInText(identifier);
        if (doi.isPresent()) {
            return new DoiFetcher(importFormatPreferences).performSearchById(doi.get().getNormalized());
        }
        Optional<ArXivIdentifier> arXivIdentifier = ArXivIdentifier.parse(identifier);
        if (arXivIdentifier.isPresent()) {
            return new ArXivFetcher(importFormatPreferences).performSearchById(arXivIdentifier.get().getNormalized());
        }
        Optional<ISBN> isbn = ISBN.parse(identifier);
        if (isbn.isPresent()) {
            return new IsbnFetcher(importFormatPreferences)
                    // .addRetryFetcher(new EbookDeIsbnFetcher(importFormatPreferences))
                    // .addRetryFetcher(new DoiToBibtexConverterComIsbnFetcher(importFormatPreferences))
                    .performSearchById(isbn.get().getNormalized());
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

    public static boolean containsValidId(String identifier) {
        Optional<DOI> doi = DOI.findInText(identifier);
        Optional<ArXivIdentifier> arXivIdentifier = ArXivIdentifier.parse(identifier);
        Optional<ISBN> isbn = ISBN.parse(identifier);

        return Stream.of(doi, arXivIdentifier, isbn).anyMatch(Optional::isPresent);
    }
}
