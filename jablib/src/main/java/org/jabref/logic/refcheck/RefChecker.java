package org.jabref.logic.refcheck;

import java.util.Optional;
import java.util.Set;

import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.ArXivFetcher;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Validates bibliographic entries against authoritative online sources.
///
/// For each entry 3 sources are tried in this order:
///
/// 1. DOI lookup via [DoiFetcher], using the DOI already present in the entry
/// 2. DOI discovery via [CrossRef], then fetching via [DoiFetcher]
/// 3. arXiv identifier lookup via [ArXivFetcher]
///
/// The lookup stops as soon as a [RefValidity#REAL] result is found.
/// If no source produces a real result, the best result among all attempts is returned.
/// UNSURE is considered better than FAKE.
/// Among results with the same validity, the one with the higher similarity score is preferred.
public class RefChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(RefChecker.class);

    private static final Set<EntryType> VERIFIABLE_TYPES = Set.of(
            StandardEntryType.Article,
            StandardEntryType.Book,
            StandardEntryType.InBook,
            StandardEntryType.InCollection,
            StandardEntryType.InProceedings,
            StandardEntryType.Conference,
            StandardEntryType.Proceedings,
            StandardEntryType.PhdThesis,
            StandardEntryType.MastersThesis,
            StandardEntryType.TechReport,
            StandardEntryType.Thesis,
            StandardEntryType.Report
    );

    /// A score below this but above zero means we found something but it does not match well.
    /// This boundary separates UNSURE from FAKE when a candidate was found.
    private static final double MIN_UNSURE_THRESHOLD = 0.5;

    private final DoiFetcher doiFetcher;
    private final ArXivFetcher arXivFetcher;
    private final CrossRef crossRef;

    /// Creates a RefChecker using only the two main fetchers.
    /// CrossRef is created with its default constructor.
    public RefChecker(DoiFetcher doiFetcher, ArXivFetcher arXivFetcher) {
        this(doiFetcher, arXivFetcher, new CrossRef());
    }

    /// Creates a RefChecker with all three dependencies supplied explicitly
    ///
    /// @param doiFetcher   the DOI fetcher
    /// @param arXivFetcher the arXiv fetcher
    /// @param crossRef     the CrossRef fetcher used for DOI discovery
    public RefChecker(DoiFetcher doiFetcher, ArXivFetcher arXivFetcher, CrossRef crossRef) {
        this.doiFetcher = doiFetcher;
        this.arXivFetcher = arXivFetcher;
        this.crossRef = crossRef;
    }

    /// Checks a single entry against authoritative online sources and returns a classification.
    ///
    /// The three sources are tried in order. If any of them produces a [RefValidity#REAL] result,
    /// that result is returned immediately without trying the remaining sources.
    ///
    /// If none of the sources produce a real result, the best result from all three attempts is returned.
    ///
    /// Individual fetcher failures are logged and treated as "not found" so that
    /// a network error from one source does not make the other sources fail
    /// Fetch failures always produce [RefValidity#FAKE] with a null matchedEntry and score 0.0.
    public RefCheckResult check(BibEntry entry) {
        if (!VERIFIABLE_TYPES.contains(entry.getType())) {
            return new RefCheckResult(RefValidity.UNSURE, null, 0.0);
        }

        RefCheckResult doiResult = checkByDoi(entry);
        if (doiResult.validity() == RefValidity.REAL) {
            return doiResult;
        }

        RefCheckResult crossRefResult = checkByCrossRef(entry);
        if (crossRefResult.validity() == RefValidity.REAL) {
            return crossRefResult;
        }

        RefCheckResult arXivResult = checkByArXiv(entry);
        if (arXivResult.validity() == RefValidity.REAL) {
            return arXivResult;
        }

        return bestOf(doiResult, crossRefResult, arXivResult);
    }

    /// Tries to validate the entry using the DOI already stored in it.
    /// Returns a FAKE result with no matched entry if the entry has no DOI
    private RefCheckResult checkByDoi(BibEntry entry) {
        Optional<DOI> doi = entry.getDOI();
        if (doi.isEmpty()) {
            return new RefCheckResult(RefValidity.FAKE, null, 0.0);
        }

        Optional<BibEntry> found;
        try {
            found = doiFetcher.performSearchById(doi.get().asString());
        } catch (FetcherException e) {
            LOGGER.warn("DOI lookup failed for {}", doi.get().asString(), e);
            return new RefCheckResult(RefValidity.FAKE, null, 0.0);
        }

        if (found.isEmpty()) {
            return new RefCheckResult(RefValidity.FAKE, null, 0.0);
        }

        return classify(entry, found.get());
    }

    /// Tries to validate the entry by first discovering a DOI through CrossRef,
    /// then fetching the authoritative entry using that DOI.
    /// Returns a FAKE result with no matched entry if CrossRef finds no DOI
    private RefCheckResult checkByCrossRef(BibEntry entry) {
        Optional<DOI> foundDoi;
        try {
            foundDoi = crossRef.findIdentifier(entry);
        } catch (FetcherException e) {
            LOGGER.warn("CrossRef lookup failed", e);
            return new RefCheckResult(RefValidity.FAKE, null, 0.0);
        }

        if (foundDoi.isEmpty()) {
            return new RefCheckResult(RefValidity.FAKE, null, 0.0);
        }

        Optional<BibEntry> found;
        try {
            found = doiFetcher.performSearchById(foundDoi.get().asString());
        } catch (FetcherException e) {
            LOGGER.warn("DOI fetch after CrossRef discovery failed for {}", foundDoi.get().asString(), e);
            return new RefCheckResult(RefValidity.FAKE, null, 0.0);
        }

        if (found.isEmpty()) {
            return new RefCheckResult(RefValidity.FAKE, null, 0.0);
        }

        return classify(entry, found.get());
    }

    /// Tries to validate the entry using arXiv.
    /// First looks for an arXiv identifier, then fetches the full entry by that identifier.
    /// Returns a FAKE result with no matched entry if no arXiv identifier is found
    private RefCheckResult checkByArXiv(BibEntry entry) {
        Optional<ArXivIdentifier> identifier;
        try {
            identifier = arXivFetcher.findIdentifier(entry);
        } catch (FetcherException e) {
            LOGGER.warn("arXiv identifier lookup failed", e);
            return new RefCheckResult(RefValidity.FAKE, null, 0.0);
        }

        if (identifier.isEmpty()) {
            return new RefCheckResult(RefValidity.FAKE, null, 0.0);
        }

        Optional<BibEntry> found;
        try {
            found = arXivFetcher.performSearchById(identifier.get().asString());
        } catch (FetcherException e) {
            LOGGER.warn("arXiv fetch failed for {}", identifier.get().asString(), e);
            return new RefCheckResult(RefValidity.FAKE, null, 0.0);
        }

        if (found.isEmpty()) {
            return new RefCheckResult(RefValidity.FAKE, null, 0.0);
        }

        return classify(entry, found.get());
    }

    /// Compares the local entry against the authoritative entry and returns a classified result.
    ///
    /// Classification rules:
    /// - [RefValidity#REAL]: score >= [DuplicateCheck#COMPARE_ENTRIES_THRESHOLD] (0.8)
    /// - [RefValidity#UNSURE]: score >= 0.5 and < 0.8
    /// - [RefValidity#FAKE]: score < 0.5 (candidate found but too different)
    ///
    /// FAKE with a non-null matchedEntry means a candidate was found but did not match.
    /// FAKE with null matchedEntry means nothing was found at all.
    private static RefCheckResult classify(BibEntry local, BibEntry authoritative) {
        double score = DuplicateCheck.compareEntries(local, authoritative);

        if (score >= DuplicateCheck.COMPARE_ENTRIES_THRESHOLD) {
            return new RefCheckResult(RefValidity.REAL, authoritative, score);
        }

        if (score >= MIN_UNSURE_THRESHOLD) {
            return new RefCheckResult(RefValidity.UNSURE, authoritative, score);
        }

        return new RefCheckResult(RefValidity.FAKE, authoritative, score);
    }

    /// Returns the best result from the three attempts.
    ///
    /// REAL > UNSURE > FAKE.
    /// Among results with the same validity the one with the higher similarity score wins.
    ///
    /// This method is only called when none of the three results is REAL.
    private static RefCheckResult bestOf(RefCheckResult doiResult, RefCheckResult crossRefResult,
                                         RefCheckResult arXivResult) {
        RefCheckResult best = doiResult;

        if (rank(crossRefResult) > rank(best)
                || (rank(crossRefResult) == rank(best)
                && crossRefResult.similarityScore() > best.similarityScore())) {
            best = crossRefResult;
        }

        if (rank(arXivResult) > rank(best)
                || (rank(arXivResult) == rank(best)
                && arXivResult.similarityScore() > best.similarityScore())) {
            best = arXivResult;
        }

        return best;
    }

    /// REAL > UNSURE > FAKE
    private static int rank(RefCheckResult result) {
        return switch (result.validity()) {
            case REAL ->
                    2;
            case UNSURE ->
                    1;
            case FAKE ->
                    0;
        };
    }
}
