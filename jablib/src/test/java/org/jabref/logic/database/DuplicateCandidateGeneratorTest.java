package org.jabref.logic.database;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.database.DuplicateCandidateGenerator.CandidatePair;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DuplicateCandidateGeneratorTest {

    private static final int FORCE_BLOCKING = 0;

    private static boolean containsPair(List<CandidatePair> pairs, BibEntry one, BibEntry two) {
        return pairs.stream().anyMatch(pair ->
                ((pair.first() == one) && (pair.second() == two)) || ((pair.first() == two) && (pair.second() == one)));
    }

    @Test
    public void smallLibraryIsSearchedExhaustively() {
        List<BibEntry> entries = List.of(
                new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "Alpha"),
                new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "Beta"),
                new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "Gamma"),
                new BibEntry(StandardEntryType.Article));

        List<CandidatePair> pairs = DuplicateCandidateGenerator.getCandidatePairs(entries).toList();

        assertEquals(6, pairs.size());
    }

    @Test
    public void sameDoiInDifferentFormsIsBlockedTogether() {
        BibEntry plainDoi = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Some title")
                .withField(StandardField.DOI, "10.1000/182");
        BibEntry doiAsUrl = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.TITLE, "Entirely different words here")
                .withField(StandardField.DOI, "https://doi.org/10.1000/182");

        List<CandidatePair> pairs = DuplicateCandidateGenerator.getCandidatePairs(List.of(plainDoi, doiAsUrl), FORCE_BLOCKING).toList();

        assertTrue(containsPair(pairs, plainDoi, doiAsUrl));
    }

    @Test
    public void typoInFirstTitleWordIsBlockedViaLaterWords() {
        BibEntry correct = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Innovation and Intellectual Property Rights");
        BibEntry withTypo = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Inovation and Intellectual Property Rights");

        List<CandidatePair> pairs = DuplicateCandidateGenerator.getCandidatePairs(List.of(correct, withTypo), FORCE_BLOCKING).toList();

        assertTrue(containsPair(pairs, correct, withTypo));
    }

    @Test
    public void sameFirstAuthorIsBlockedTogetherDespiteDifferentTitles() {
        BibEntry one = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Smith, John and Doe, Jane")
                .withField(StandardField.TITLE, "One title");
        BibEntry two = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "John Smith")
                .withField(StandardField.TITLE, "Completely other words");

        List<CandidatePair> pairs = DuplicateCandidateGenerator.getCandidatePairs(List.of(one, two), FORCE_BLOCKING).toList();

        assertTrue(containsPair(pairs, one, two));
    }

    @Test
    public void unrelatedEntriesAreNotBlockedTogether() {
        BibEntry one = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Smith, John")
                .withField(StandardField.TITLE, "Innovation and Intellectual Property Rights")
                .withField(StandardField.DOI, "10.1000/182");
        BibEntry two = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Doe, Jane")
                .withField(StandardField.TITLE, "A serious paper about something")
                .withField(StandardField.DOI, "10.1000/183");

        List<CandidatePair> pairs = DuplicateCandidateGenerator.getCandidatePairs(List.of(one, two), FORCE_BLOCKING).toList();

        assertFalse(containsPair(pairs, one, two));
    }

    @Test
    public void entryWithoutAnyKeyIsComparedWithAllOtherEntries() {
        BibEntry withoutKeys = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.JOURNAL, "International Journal of Something")
                .withField(StandardField.YEAR, "2017");
        BibEntry regular = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Smith, John")
                .withField(StandardField.TITLE, "Innovation and Intellectual Property Rights");

        List<CandidatePair> pairs = DuplicateCandidateGenerator.getCandidatePairs(List.of(withoutKeys, regular), FORCE_BLOCKING).toList();

        assertTrue(containsPair(pairs, withoutKeys, regular));
    }

    @Test
    public void pairsAreNotReportedTwiceWhenSharingSeveralKeys() {
        BibEntry one = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Smith, John")
                .withField(StandardField.TITLE, "Innovation and Intellectual Property Rights");
        BibEntry two = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Smith, J.")
                .withField(StandardField.TITLE, "Innovation and Intellectual Property Rights");

        List<CandidatePair> pairs = DuplicateCandidateGenerator.getCandidatePairs(List.of(one, two), FORCE_BLOCKING).toList();

        assertEquals(1, pairs.size());
    }

    @Test
    public void sameIsbnWithDifferentCheckDigitCaseIsBlockedTogether() {
        BibEntry upperCase = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.ISBN, "0-8044-2957-X");
        BibEntry lowerCase = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.ISBN, "0-8044-2957-x");

        List<CandidatePair> pairs = DuplicateCandidateGenerator.getCandidatePairs(List.of(upperCase, lowerCase), FORCE_BLOCKING).toList();

        assertTrue(containsPair(pairs, upperCase, lowerCase));
    }

    /// A shared identifier alone makes a pair a duplicate, so identifier blocks must survive the
    /// large-block safeguard.
    @Test
    public void oversizedIdentifierBlockIsNotDropped() {
        List<BibEntry> entries = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            entries.add(new BibEntry(StandardEntryType.Misc)
                    .withField(StandardField.DOI, "10.1000/shared"));
        }

        long pairCount = DuplicateCandidateGenerator.getCandidatePairs(entries, FORCE_BLOCKING).count();

        assertEquals((150L * 149) / 2, pairCount);
    }

    /// Reports the candidate reduction for typical metadata shapes (guardrail from
    /// [issue 16579](https://github.com/JabRef/jabref/issues/16579)): the number of candidate pairs
    /// must stay far below the exhaustive `n * (n - 1) / 2`, including a common-field worst case
    /// where all entries share the same journal and year.
    @Test
    public void blockingStaysFarBelowExhaustivePairCount() {
        // [utest->req~logic.duplicates.candidate-blocking~1]
        int entryCount = 2000;
        long exhaustivePairs = ((long) entryCount * (entryCount - 1)) / 2;
        List<BibEntry> complete = new ArrayList<>();
        List<BibEntry> sparse = new ArrayList<>();
        List<BibEntry> commonFields = new ArrayList<>();
        for (int i = 0; i < entryCount; i++) {
            complete.add(new BibEntry(StandardEntryType.Article)
                    .withField(StandardField.AUTHOR, "Author" + i + ", First")
                    .withField(StandardField.TITLE, "Distinct" + i + " title" + i + " about" + i + " topic" + i)
                    .withField(StandardField.DOI, "10.1000/" + i)
                    .withField(StandardField.YEAR, "2020"));
            sparse.add(new BibEntry(StandardEntryType.Misc)
                    .withField(StandardField.TITLE, "Sparse" + i));
            // Worst case: all entries share the journal, the year, and the leading title word
            commonFields.add(new BibEntry(StandardEntryType.Article)
                    .withField(StandardField.AUTHOR, "Writer" + i + ", W.")
                    .withField(StandardField.TITLE, "The unique" + i + " observations" + i)
                    .withField(StandardField.JOURNAL, "Journal of Common Fields")
                    .withField(StandardField.YEAR, "1999"));
        }

        assertTrue(DuplicateCandidateGenerator.getCandidatePairs(complete, FORCE_BLOCKING).count() < (exhaustivePairs / 100));
        assertTrue(DuplicateCandidateGenerator.getCandidatePairs(sparse, FORCE_BLOCKING).count() < (exhaustivePairs / 100));
        assertTrue(DuplicateCandidateGenerator.getCandidatePairs(commonFields, FORCE_BLOCKING).count() < (exhaustivePairs / 100));
    }

    /// Blocking must not lose pairs that [DuplicateCheck] classifies as duplicates:
    /// every duplicate pair found by the exhaustive search must also be a blocking candidate.
    @Test
    public void blockingRetainsAllDuplicatePairsFoundExhaustively() {
        // [utest->req~logic.duplicates.candidate-blocking~1]
        List<BibEntry> entries = List.of(
                new BibEntry(StandardEntryType.Article)
                        .withField(StandardField.AUTHOR, "Single Author")
                        .withField(StandardField.TITLE, "A serious paper about something")
                        .withField(StandardField.YEAR, "2017"),
                new BibEntry(StandardEntryType.Article)
                        .withField(StandardField.AUTHOR, "Single Author")
                        .withField(StandardField.TITLE, "A serious paper about something")
                        .withField(StandardField.YEAR, "2017")
                        .withField(StandardField.JOURNAL, "Journal of Serious Papers"),
                new BibEntry(StandardEntryType.InCollection)
                        .withField(StandardField.TITLE, "Innovation and Intellectual Property Rights")
                        .withField(StandardField.AUTHOR, "Ove Grandstrand")
                        .withField(StandardField.YEAR, "2004"),
                new BibEntry(StandardEntryType.InProceedings)
                        .withField(StandardField.TITLE, "Innovation and Intellectual Property Rights")
                        .withField(StandardField.AUTHOR, "Grandstrand, Ove")
                        .withField(StandardField.DOI, "10.1000/999"),
                new BibEntry(StandardEntryType.Misc)
                        .withField(StandardField.DOI, "https://doi.org/10.1000/999"),
                new BibEntry(StandardEntryType.Article)
                        .withField(StandardField.AUTHOR, "Completely Different")
                        .withField(StandardField.TITLE, "Holiday Ideas for the Whole Family")
                        .withField(StandardField.YEAR, "1992"));

        DuplicateCheck duplicateCheck = new DuplicateCheck(new BibEntryTypesManager());
        List<CandidatePair> blockedPairs = DuplicateCandidateGenerator.getCandidatePairs(entries, FORCE_BLOCKING).toList();

        for (int i = 0; i < (entries.size() - 1); i++) {
            for (int j = i + 1; j < entries.size(); j++) {
                BibEntry first = entries.get(i);
                BibEntry second = entries.get(j);
                if (duplicateCheck.isDuplicate(first, second, BibDatabaseMode.BIBTEX)) {
                    assertTrue(containsPair(blockedPairs, first, second),
                            "Blocking lost the duplicate pair %d/%d".formatted(i, j));
                }
            }
        }
    }
}
