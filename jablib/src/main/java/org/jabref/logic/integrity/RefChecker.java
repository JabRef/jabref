package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.plaincitation.PlainCitationParser;
import org.jabref.logic.importer.plaincitation.SeveralPlainCitationParser;
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
        this.doiFetcher = doiFetcher;
        this.arxivFetcher = arXivFetcher;
        this.crossRef = crossRef;
        this.duplicateCheck = duplicateCheck;
    }

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

    public Map<BibEntry, ReferenceValidity> validateListOfEntries(List<BibEntry> entries) throws FetcherException {

        Map<BibEntry, ReferenceValidity> entriesToValidity = new HashMap<>();
        for (BibEntry entry : entries) {
            entriesToValidity.put(entry, referenceValidityOfEntry(entry));
        }
        return entriesToValidity;
    }

    public Map<BibEntry, ReferenceValidity> parseListAndValidate(String input, PlainCitationParser parser) throws FetcherException {
        SeveralPlainCitationParser citationParser = new SeveralPlainCitationParser(parser);
        return validateListOfEntries(citationParser.parseSeveralPlainCitations(input));
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

    public static abstract sealed class ReferenceValidity permits Real, Unsure, Fake {

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

    public static final class Real extends ReferenceValidity {
        BibEntry matchingReference;

        public Real(BibEntry matchingReference) {
            this.matchingReference = matchingReference;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Real real = (Real) o;
            return Objects.equals(matchingReference, real.matchingReference);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(matchingReference);
        }
    }

    public static final class Unsure extends ReferenceValidity {
        List<BibEntry> matchingReferences;

        public Unsure(BibEntry matchingReference) {
            List<BibEntry> matchingReferences = new ArrayList<>();
            matchingReferences.add(matchingReference);
            this.matchingReferences = matchingReferences;
        }

        void addAll(Unsure other) {
            this.matchingReferences.addAll(other.matchingReferences);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Unsure unsure = (Unsure) o;
            return Objects.equals(matchingReferences, unsure.matchingReferences);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(matchingReferences);
        }
    }

    public static final class Fake extends ReferenceValidity {
        public boolean equals(Object o) {
            return o.getClass() == Fake.class;
        }
    }
}
