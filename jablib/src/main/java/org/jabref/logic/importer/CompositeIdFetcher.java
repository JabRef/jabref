package org.jabref.logic.importer;

import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.RfcFetcher;
import org.jabref.logic.importer.fetcher.isbntobibtex.IsbnFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.model.entry.identifier.RFC;
import org.jabref.model.entry.identifier.SSRN;

public class CompositeIdFetcher {

    private final ImportFormatPreferences importFormatPreferences;

    public CompositeIdFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        // All identifiers listed here should also be appear at {@link org.jabref.gui.mergeentries.FetchAndMergeEntry.SUPPORTED_FIELDS} and vice versa.

        Optional<DOI> doi = DOI.findInText(identifier);
        if (doi.isPresent()) {
            return new DoiFetcher(importFormatPreferences).performSearchById(doi.get().asString());
        }
        Optional<ArXivIdentifier> arXivIdentifier = ArXivIdentifier.parse(identifier);
        if (arXivIdentifier.isPresent()) {
            return new ArXivFetcher(importFormatPreferences).performSearchById(arXivIdentifier.get().asString());
        }
        Optional<ISBN> isbn = ISBN.parse(identifier);
        if (isbn.isPresent()) {
            return new IsbnFetcher(importFormatPreferences)
                    // .addRetryFetcher(new EbookDeIsbnFetcher(importFormatPreferences))
                    // .addRetryFetcher(new DoiToBibtexConverterComIsbnFetcher(importFormatPreferences))
                    .performSearchById(isbn.get().asString());
        }
        /* TODO: IACR is currently disabled, because it needs to be reworked: https://github.com/JabRef/jabref/issues/8876
        Optional<IacrEprint> iacrEprint = IacrEprint.parse(identifier);
        if (iacrEprint.isPresent()) {
            return new IacrEprintFetcher(importFormatPreferences).performSearchById(iacrEprint.get().getNormalized());
        }*/

        Optional<SSRN> ssrn = SSRN.parse(identifier);
        if (ssrn.isPresent()) {
            return new DoiFetcher(importFormatPreferences).performSearchById(ssrn.get().toDoi().asString());
        }

        Optional<RFC> rfcId = RFC.parse(identifier);
        if (rfcId.isPresent()) {
            return new RfcFetcher(importFormatPreferences).performSearchById(rfcId.get().asString());
        }

        return Optional.empty();
    }

    public String getName() {
        return "CompositeIdFetcher";
    }

    public static boolean containsValidId(String identifier) {
        Optional<DOI> doi = DOI.findInText(identifier);
        Optional<ArXivIdentifier> arXivIdentifier = ArXivIdentifier.parse(identifier);
        Optional<ISBN> isbn = ISBN.parse(identifier);
        Optional<SSRN> ssrn = SSRN.parse(identifier);
        Optional<RFC> rfcId = RFC.parse(identifier);

        return Stream.of(doi, arXivIdentifier, isbn, ssrn, rfcId).anyMatch(Optional::isPresent);
    }
}
