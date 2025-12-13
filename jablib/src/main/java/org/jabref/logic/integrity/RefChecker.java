package org.jabref.logic.integrity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;

import com.airhacks.afterburner.injection.Injector;

/**
 * Validates a BibEntry depending on if it
 * is consistent with the fetched Entry
 */
public class RefChecker {
    DoiFetcher doiFetcher;
    ArXivFetcher arxivFetcher;
    CrossRef crossRef;
    DuplicateCheck duplicateCheck;

    public RefChecker(DoiFetcher doiFetcher, ArXivFetcher arXivFetcher) {
        this(doiFetcher, arXivFetcher, new CrossRef(), new DuplicateCheck(Injector.instantiateModelOrService(BibEntryTypesManager.class)));
    }

    public RefChecker(
            DoiFetcher doiFetcher,
            ArXivFetcher arXivFetcher,
            CrossRef crossRef,
            DuplicateCheck duplicateCheck) {
        this.doiFetcher = doiFetcher;
        this.arxivFetcher = arXivFetcher;
        this.crossRef = crossRef;
        this.duplicateCheck = duplicateCheck;
    }

    /**
     * Tries to find the best reference validity
     * among current ways. If any of the methods signal
     * that it is real, it returns early.
     * <p>
     * DoiFetcher -> CrossRef -> ArxivFetcher
     *
     * @param entry entry checking
     * @return the reference validity
     * @throws FetcherException any error from fetchers
     */
    public ReferenceValidity referenceValidityOfEntry(BibEntry entry) throws FetcherException {
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
        return other.map(foundEntry -> compareReferences(entry, foundEntry))
                    .orElse(new Fake());
    }

    /**
     * Tests validity only from the DoiFetcher.
     *
     * @param entry the entry
     * @return the reference validity
     * @throws FetcherException the fetcher exception
     */
    public ReferenceValidity validityFromDoiFetcher(BibEntry entry) throws FetcherException {
        return validityFromFetcher(entry, doiFetcher);
    }

    /**
     * Validity only from the CrossRef and later from the DoiFetcher.
     *
     * @param entry the entry
     * @return the reference validity
     * @throws FetcherException the fetcher exception
     */
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

    /**
     * Validity only from the arxivFetcher.
     *
     * @param entry the entry
     * @return the reference validity
     * @throws FetcherException the fetcher exception
     */
    public ReferenceValidity validityFromArxiv(BibEntry entry) throws FetcherException {

        Optional<ArXivIdentifier> foundIdentifier = arxivFetcher.findIdentifier(entry);
        if (foundIdentifier.isEmpty()) {
            return new Fake();
        }
        return arxivFetcher.performSearchById(foundIdentifier.get().asString()).map(
                foundEntry -> compareReferences(entry, foundEntry)
        ).orElse(new Fake());
    }

    /**
     * Takes a list for entries and returns the mapping of them with their corresponding
     * reference validity.
     *
     * @param entries the entries
     * @return the map
     * @throws FetcherException the fetcher exception
     */
    public Map<BibEntry, ReferenceValidity> validateListOfEntries(List<BibEntry> entries) throws FetcherException {

        Map<BibEntry, ReferenceValidity> entriesToValidity = new HashMap<>();
        for (BibEntry entry : entries) {
            entriesToValidity.put(entry, referenceValidityOfEntry(entry));
        }
        return entriesToValidity;
    }

    private ReferenceValidity compareReferences(BibEntry localEntry, BibEntry validFoundEntry) {
        double similarity = duplicateCheck.degreeOfSimilarity(localEntry, validFoundEntry);
        if (similarity >= 0.999) {
            return new Real(validFoundEntry);
        } else if (similarity > 0.8) {
            return new Unsure(validFoundEntry);
        } else {
            return new Fake();
        }
    }

    @FunctionalInterface
    private interface ReferenceValiditySupplier {
        ReferenceValidity get() throws FetcherException;
    }

    public static abstract sealed class ReferenceValidity permits Real, Unsure, Fake {

        public ReferenceValidity or(ReferenceValidity other) {
            if (this instanceof Real || other instanceof Fake) {
                return this;
            }
            if (other instanceof Unsure otherUnsure && this instanceof Unsure thisUnsure) {
                Unsure merge = new Unsure();
                merge.addAll(thisUnsure);
                merge.addAll(otherUnsure);
                return merge;
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

    public static final class Real extends ReferenceValidity {
        BibEntry matchingReference;

        public Real(BibEntry matchingReference) {
            this.matchingReference = matchingReference;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Real real = (Real) o;
            return Objects.equals(matchingReference, real.matchingReference);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(matchingReference);
        }

        public BibEntry getMatchingReference() {
            return matchingReference;
        }
    }

    public static final class Unsure extends ReferenceValidity {
        Set<BibEntry> matchingReferences;

        public Unsure(BibEntry matchingReference) {
            this.matchingReferences = new HashSet<>(Set.of(matchingReference));
        }

        private Unsure() {
            this.matchingReferences = new HashSet<>();
        }

        void addAll(Unsure other) {
            this.matchingReferences.addAll(other.matchingReferences);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Unsure unsure = (Unsure) o;
            return Objects.equals(matchingReferences, unsure.matchingReferences);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(matchingReferences);
        }

        public Set<BibEntry> getMatchingReferences() {
            return matchingReferences;
        }
    }

    public static final class Fake extends ReferenceValidity {
        public boolean equals(Object o) {
            return o.getClass() == Fake.class;
        }

        public int hashCode() {
            return Objects.hashCode(Fake.class);
        }
    }
}
