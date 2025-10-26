package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.identifier.DOI;

/**
 * Validates a BibEntry depending on if it
 * is consistent with the fetched Entry
 */
public class RefChecker {
    DoiFetcher doiFetcher;
    ArXivFetcher arxivFetcher;
    CrossRef crossRef;
    DuplicateCheck duplicateCheck;

    public RefChecker(
            DoiFetcher doiFetcher,
            ArXivFetcher arXivFetcher) {
        this(doiFetcher, arXivFetcher, new CrossRef(), new DuplicateCheck(new BibEntryTypesManager()));
    }

    public RefChecker(
            DoiFetcher doiFetcher,
            ArXivFetcher arXivFetcher,
            CrossRef crossRef,
            DuplicateCheck duplicateCheck) {
        this.parser = parser;
        this.doiFetcher = doiFetcher;
        this.arxivFetcher = arXivFetcher;
        this.crossRef = crossRef;
        this.duplicateCheck = duplicateCheck;
    }

    private ReferenceValidity referenceValidityOfEntry(BibEntry entry) throws FetcherException {
        return validityFromDoiFetcher(entry).lazyOr(() ->
                validityFromCrossRef(entry)
        ).lazyOr(() -> validityFromArxiv(entry));
    }

    private ReferenceValidity validityFromFetcher(BibEntry entry, IdBasedFetcher fetcher) throws FetcherException {
        Optional<DOI> doi = entry.getDOI();
        if (doi.isEmpty()) {
            return new Fake();
        }

        Optional<BibEntry> other = fetcher.performSearchById(doi.get().asString());
        return other.map(o -> compareReferences(entry, o))
                    .orElse(new Fake());
    }

    public ReferenceValidity validityFromDoiFetcher(BibEntry entry) throws FetcherException {
        return validityFromFetcher(entry, doiFetcher);
    }

    public ReferenceValidity validityFromCrossRef(BibEntry entry) throws FetcherException {
        Optional<DOI> doiFound = crossRef.findIdentifier(entry);

        if (doiFound.isEmpty()) {
            return new Fake();
        } else {
            DOI doi = doiFound.get();
            return doiFetcher.performSearchById(doi.asString()).map(
                    (found) -> compareReferences(entry, found)
            ).orElse(new Fake());
        }
    }

    public ReferenceValidity validityFromArxiv(BibEntry entry) throws FetcherException {

        var m = arxivFetcher.findIdentifier(entry);
        if (m.isEmpty()) {
            return new Fake();
        }
        return arxivFetcher.performSearchById(m.get().asString()).map(
                found -> compareReferences(entry, found)
        ).orElse(new Fake());
    }

    private ReferenceValidity compareReferences(BibEntry original, BibEntry trueEntry) {
        if (duplicateCheck.isDuplicate(original, trueEntry, BibDatabaseMode.BIBTEX)) {
            return new Real(trueEntry);
        } else {
            return new Fake();
        }
    }

    @FunctionalInterface
    private interface ReferenceValiditySupplier {
        ReferenceValidity get() throws FetcherException;
    }

    public abstract sealed class ReferenceValidity permits Real, Unsure, Fake {

        public ReferenceValidity or(ReferenceValidity other) {
            if (this instanceof Real || other instanceof Fake) {
                return this;
            }
            if (other instanceof Unsure otherUnsure && this instanceof Unsure thisUnsure) {
                otherUnsure.addAll(thisUnsure);
            }
            return other;
        }

        private ReferenceValidity lazyOr(ReferenceValiditySupplier other) throws FetcherException {
            if (this instanceof Real) {
                return this;
            } else {
                return or(other.get());
            }
        }
    }

    public final class Real extends ReferenceValidity {
        BibEntry matchingReference;

        public Real(BibEntry matchingReference) {
            this.matchingReference = matchingReference;
        }
    }

    public final class Unsure extends ReferenceValidity {
        List<BibEntry> matchingReferences;

        public Unsure(BibEntry matchingReference) {
            List<BibEntry> matchingReferences = new ArrayList<>();
            matchingReferences.add(matchingReference);
            this.matchingReferences = matchingReferences;
        }

        void addAll(Unsure other) {
            this.matchingReferences.addAll(other.matchingReferences);
        }
    }

    public final class Fake extends ReferenceValidity {
    }
}
