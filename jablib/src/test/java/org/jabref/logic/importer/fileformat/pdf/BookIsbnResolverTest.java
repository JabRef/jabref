package org.jabref.logic.importer.fileformat.pdf;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@NullMarked
class BookIsbnResolverTest {

    private static final ISBN MATCHING_ISBN = new ISBN("9780596520687");
    private static final ISBN OTHER_ISBN = new ISBN("9780134685991");

    private final BookIsbnResolver resolver = new BookIsbnResolver();

    // [utest->req~import.pdf.book-isbn-resolution~1]
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void selectsUniqueMatchRegardlessOfCandidateOrder(boolean reverseOrder) {
        Map<ISBN, Optional<BibEntry>> candidates = new LinkedHashMap<>();
        List<ISBN> isbns = List.of(MATCHING_ISBN, OTHER_ISBN);
        if (reverseOrder) {
            isbns = isbns.reversed();
        }
        for (ISBN isbn : isbns) {
            if (isbn.equals(MATCHING_ISBN)) {
                candidates.put(isbn, Optional.of(matchingBook()));
            } else {
                candidates.put(isbn, Optional.of(differentYearBook()));
            }
        }

        assertEquals(Optional.of(MATCHING_ISBN), resolver.resolve(localBook(), candidates));
    }

    // [utest->req~import.pdf.book-isbn-resolution~1]
    @ParameterizedTest
    @ValueSource(ints = {4, 5})
    void resolvesAtMostFourCandidates(int candidateCount) {
        List<ISBN> isbns = List.of(MATCHING_ISBN, OTHER_ISBN, new ISBN("9780306406157"),
                new ISBN("9789660800007"), new ISBN("9798602405453"));
        Map<ISBN, Optional<BibEntry>> candidates = new LinkedHashMap<>();
        candidates.put(MATCHING_ISBN, Optional.of(matchingBook()));
        for (ISBN isbn : isbns.subList(1, candidateCount)) {
            candidates.put(isbn, Optional.of(differentYearBook().withField(StandardField.ISBN, isbn.asString())));
        }

        assertEquals(candidateCount == 4 ? Optional.of(MATCHING_ISBN) : Optional.empty(),
                resolver.resolve(localBook(), candidates));
    }

    @Test
    void normalizesPunctuationCaseAndContributorOrderWithinNames() {
        BibEntry candidate = matchingBook()
                .withField(StandardField.TITLE, "{ЗБІРНИК}  задач — з фізики")
                .withField(StandardField.AUTHOR, "Коваль, О. П.")
                .withField(StandardField.PUBLISHER, "РАНОК.");

        assertEquals(Optional.of(MATCHING_ISBN), resolve(candidate, differentYearBook()));
    }

    @Test
    void acceptsEquivalentReturnedIsbn10() {
        BibEntry candidate = matchingBook().withField(StandardField.ISBN, "0-596-52068-9");

        assertEquals(Optional.of(MATCHING_ISBN), resolve(candidate, differentYearBook()));
    }

    @Test
    void retainsAmbiguityForTwoMatchingFormats() {
        BibEntry otherFormat = localBook().withField(StandardField.ISBN, OTHER_ISBN.asString());

        assertEquals(Optional.empty(), resolve(matchingBook(), otherFormat));
    }

    @Test
    void retainsAmbiguityWhenAlternativeLookupIsMissing() {
        assertEquals(Optional.empty(), resolver.resolve(localBook(), Map.of(
                MATCHING_ISBN, Optional.of(matchingBook()),
                OTHER_ISBN, Optional.empty())));
    }

    @ParameterizedTest
    @EnumSource(value = StandardField.class, names = {"TITLE", "AUTHOR", "PUBLISHER", "YEAR", "ISBN"})
    void retainsAmbiguityWhenAlternativeLookupIsIncomplete(StandardField missingField) {
        BibEntry alternative = differentYearBook();
        alternative.clearField(missingField);

        assertEquals(Optional.empty(), resolve(matchingBook(), alternative));
    }

    @ParameterizedTest
    @EnumSource(value = StandardField.class, names = {"TITLE", "AUTHOR", "PUBLISHER", "YEAR"})
    void retainsAmbiguityWhenLocalEvidenceIsMissing(StandardField missingField) {
        BibEntry local = localBook();
        local.clearField(missingField);

        assertEquals(Optional.empty(), resolver.resolve(local, Map.of(
                MATCHING_ISBN, Optional.of(matchingBook()),
                OTHER_ISBN, Optional.of(differentYearBook()))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "..."})
    void retainsAmbiguityWhenLocalTitleHasNoUsableText(String title) {
        BibEntry local = localBook().withField(StandardField.TITLE, title);

        assertFalse(resolver.hasRequiredMetadata(local));
    }

    @Test
    void rejectsReturnedIdentifierForAnotherCandidate() {
        BibEntry alternative = differentYearBook().withField(StandardField.ISBN, MATCHING_ISBN.asString());

        assertEquals(Optional.empty(), resolve(matchingBook(), alternative));
    }

    @Test
    void retainsAmbiguityForPublisherDifferenceAlone() {
        BibEntry alternative = localBook()
                .withField(StandardField.ISBN, OTHER_ISBN.asString())
                .withField(StandardField.PUBLISHER, "Another publisher");

        assertEquals(Optional.empty(), resolve(matchingBook(), alternative));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2010/2011", "2010?", "2010 [2011]", "forthcoming"})
    void retainsAmbiguityForUncertainPublicationYears(String year) {
        BibEntry alternative = differentYearBook().withField(StandardField.YEAR, year);

        assertEquals(Optional.empty(), resolve(matchingBook(), alternative));
    }

    @Test
    void rejectsCandidateWhenAllComparedFieldsDiffer() {
        BibEntry unrelatedBook = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.ISBN, OTHER_ISBN.asString())
                .withField(StandardField.TITLE, "Effective Java")
                .withField(StandardField.AUTHOR, "Joshua Bloch")
                .withField(StandardField.PUBLISHER, "Addison-Wesley")
                .withField(StandardField.YEAR, "2018");

        assertEquals(Optional.of(MATCHING_ISBN), resolve(matchingBook(), unrelatedBook));
    }

    @Test
    void selectsNothingWhenNeitherCandidateMatches() {
        assertEquals(Optional.empty(), resolve(
                matchingBook().withField(StandardField.YEAR, "1990"), differentYearBook()));
    }

    @Test
    void comparesExplicitEditorsWhenAuthorsAreAbsent() {
        BibEntry local = localBook();
        local.clearField(StandardField.AUTHOR);
        local.withField(StandardField.EDITOR, "О. П. Коваль");
        BibEntry matching = new BibEntry(local).withField(StandardField.ISBN, MATCHING_ISBN.asString());
        BibEntry alternative = new BibEntry(local)
                .withField(StandardField.ISBN, OTHER_ISBN.asString())
                .withField(StandardField.YEAR, "1999");

        assertEquals(Optional.of(MATCHING_ISBN), resolver.resolve(local, Map.of(
                MATCHING_ISBN, Optional.of(matching), OTHER_ISBN, Optional.of(alternative))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "3"})
    void retainsAmbiguityWhenExplicitEditionCannotBeConfirmed(String fetchedEdition) {
        BibEntry local = localBook().withField(StandardField.EDITION, "2");
        BibEntry matching = matchingBook().withField(StandardField.EDITION, fetchedEdition);
        BibEntry alternative = differentYearBook().withField(StandardField.EDITION, "2");

        assertEquals(Optional.empty(), resolver.resolve(local, Map.of(
                MATCHING_ISBN, Optional.of(matching), OTHER_ISBN, Optional.of(alternative))));
    }

    private Optional<ISBN> resolve(BibEntry matching, BibEntry alternative) {
        return resolver.resolve(localBook(), Map.of(
                MATCHING_ISBN, Optional.of(matching), OTHER_ISBN, Optional.of(alternative)));
    }

    private BibEntry localBook() {
        return new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Збірник задач з фізики")
                .withField(StandardField.AUTHOR, "О. П. Коваль")
                .withField(StandardField.PUBLISHER, "Ранок")
                .withField(StandardField.YEAR, "2010");
    }

    private BibEntry matchingBook() {
        return localBook().withField(StandardField.ISBN, MATCHING_ISBN.asString());
    }

    private BibEntry differentYearBook() {
        return localBook()
                .withField(StandardField.ISBN, OTHER_ISBN.asString())
                .withField(StandardField.YEAR, "1999");
    }
}
