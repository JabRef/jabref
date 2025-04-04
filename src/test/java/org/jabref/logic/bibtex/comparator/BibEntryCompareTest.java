package org.jabref.logic.bibtex.comparator;

import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BibEntryCompareTest {

    public static Stream<Arguments> compareEntries() {
        return Stream.of(
                Arguments.of(BibEntryCompare.Result.EQUAL, new BibEntry(), new BibEntry()),
                Arguments.of(BibEntryCompare.Result.SUBSET, new BibEntry(), new BibEntry().withField(StandardField.AUTHOR, "Knuth")),
                Arguments.of(BibEntryCompare.Result.SUPERSET, new BibEntry().withField(StandardField.AUTHOR, "Knuth"), new BibEntry()),
                Arguments.of(BibEntryCompare.Result.DISJUNCT,
                        new BibEntry().withField(StandardField.AUTHOR, "Knuth"),
                        new BibEntry().withField(StandardField.EDITOR, "Mittelbach")),
                Arguments.of(BibEntryCompare.Result.DISJUNCT_OR_EQUAL_FIELDS,
                        new BibEntry().withField(StandardField.AUTHOR, "Knuth").withField(StandardField.YEAR, "2000"),
                        new BibEntry().withField(StandardField.EDITOR, "Mittelbach").withField(StandardField.YEAR, "2000")),
                Arguments.of(BibEntryCompare.Result.DIFFERENT,
                        new BibEntry().withField(StandardField.AUTHOR, "Knuth"),
                        new BibEntry().withField(StandardField.AUTHOR, "Mittelbach"))
        );
    }

    @ParameterizedTest
    @MethodSource
    void compareEntries(BibEntryCompare.Result equal, BibEntry first, BibEntry second) {
        assertEquals(equal, BibEntryCompare.compareEntries(first, second));
    }
}
