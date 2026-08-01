package org.jabref.gui.mergeentries.multiwaymerge;

import java.util.Map;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MultiMergeEntriesViewModelTest {

    private MultiMergeEntriesViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new MultiMergeEntriesViewModel();
    }

    @Test
    void updateFieldsIgnoresNullEntry() {
        viewModel.updateFields(null);
        assertEquals(new BibEntry(), viewModel.mergedEntryProperty().get());
    }

    @Test
    void updateFieldsSetsFieldWhenNotYetPresent() {
        BibEntry source = new BibEntry().withField(StandardField.YEAR, "2015");
        viewModel.updateFields(source);
        assertEquals("2015", viewModel.mergedEntryProperty().get().getField(StandardField.YEAR).orElse(""));
    }

    static Stream<Arguments> updateFieldsMergesWithPlausibility() {
        return Stream.of(
                // No comparator → first-seen value wins
                Arguments.of(StandardField.TITLE, "First Title", "Second Title", "First Title"),
                // YEAR: valid over malformed
                Arguments.of(StandardField.YEAR, "201", "2015", "2015"),
                // AUTHOR: more complete list preferred
                Arguments.of(StandardField.AUTHOR, "Wang, J.", "Wang, Jun and Smith, Alice", "Wang, Jun and Smith, Alice"),
                // MONTH: BibTeX constant preferred over plain text
                Arguments.of(StandardField.MONTH, "June", "#jun#", "#jun#"),
                // DATE: more specific date preferred
                Arguments.of(StandardField.DATE, "2017-09", "2017-09-12", "2017-09-12"),
                // DATE: valid date preferred over malformed
                Arguments.of(StandardField.DATE, "foo-bar-baz", "2017-09", "2017-09"),
                // DATE: any date preferred over absent
                Arguments.of(StandardField.DATE, null, "2017-09-12", "2017-09-12")
        );
    }

    @ParameterizedTest
    @MethodSource
    void updateFieldsMergesWithPlausibility(Field field, String leftValue, String rightValue, String expected) {
        BibEntry leftEntry = leftValue == null ? new BibEntry() : new BibEntry().withField(field, leftValue);
        BibEntry rightEntry = new BibEntry().withField(field, rightValue);

        viewModel.updateFields(leftEntry);
        viewModel.updateFields(rightEntry);

        assertEquals(expected, viewModel.mergedEntryProperty().get().getField(field).orElse(""));
    }

    @Test
    void findNewFetchableIdentifiersReturnsDoiOnce() {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1000/182");
        assertEquals(Map.of(StandardField.DOI, "10.1000/182"), viewModel.findNewFetchableIdentifiers(entry));
    }

    @Test
    void findNewFetchableIdentifiersDedupesRepeatedCall() {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "10.1000/182");
        viewModel.findNewFetchableIdentifiers(entry);
        assertEquals(Map.of(), viewModel.findNewFetchableIdentifiers(entry));
    }

    @Test
    void findNewFetchableIdentifiersDedupesCaseAndWhitespaceVariants() {
        BibEntry first = new BibEntry().withField(StandardField.DOI, "10.1000/182");
        BibEntry second = new BibEntry().withField(StandardField.DOI, "  10.1000/182  ");
        viewModel.findNewFetchableIdentifiers(first);
        assertEquals(Map.of(), viewModel.findNewFetchableIdentifiers(second));
    }

    @Test
    void findNewFetchableIdentifiersIgnoresBlankValue() {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "   ");
        assertEquals(Map.of(), viewModel.findNewFetchableIdentifiers(entry));
    }

    @Test
    void findNewFetchableIdentifiersIgnoresUnsupportedFields() {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "Some Title");
        assertEquals(Map.of(), viewModel.findNewFetchableIdentifiers(entry));
    }

    @Test
    void findNewFetchableIdentifiersDedupesUrlAndPlainFormOfSameDoi() {
        BibEntry plain = new BibEntry().withField(StandardField.DOI, "10.1145/3651640.3651646");
        BibEntry url = new BibEntry().withField(StandardField.DOI, "https://doi.org/10.1145/3651640.3651646");
        viewModel.findNewFetchableIdentifiers(plain);
        assertEquals(Map.of(), viewModel.findNewFetchableIdentifiers(url));
    }

    @Test
    void findNewFetchableIdentifiersIgnoresInvalidDoi() {
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "not-a-doi");
        assertEquals(Map.of(), viewModel.findNewFetchableIdentifiers(entry));
    }

    @Test
    void findNewFetchableIdentifiersIgnoresUnsupportedInfoDoiScheme() {
        // XMP metadata sometimes stores DOIs using the "info:doi/" URI scheme, which DOI.parse does not
        // recognize. Must be filtered out rather than passed to the fetcher as-is, where it would fail.
        BibEntry entry = new BibEntry().withField(StandardField.DOI, "info:doi/10.1145/3651640.3651646");
        assertEquals(Map.of(), viewModel.findNewFetchableIdentifiers(entry));
    }

    @Test
    void findNewFetchableIdentifiersReturnsMultipleSupportedFields() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.ISBN, "978-3-16-148410-0")
                .withField(StandardField.EPRINT, "2101.00001");
        assertEquals(
                Map.of(StandardField.ISBN, "9783161484100", StandardField.EPRINT, "2101.00001"),
                viewModel.findNewFetchableIdentifiers(entry));
    }

    @Test
    void findNewFetchableIdentifiersNormalizesIsbnDashes() {
        BibEntry hyphenated = new BibEntry().withField(StandardField.ISBN, "978-3-16-148410-0");
        BibEntry plain = new BibEntry().withField(StandardField.ISBN, "9783161484100");
        viewModel.findNewFetchableIdentifiers(hyphenated);
        assertEquals(Map.of(), viewModel.findNewFetchableIdentifiers(plain));
    }

    @Test
    void findNewFetchableIdentifiersIgnoresInvalidIsbnChecksum() {
        BibEntry entry = new BibEntry().withField(StandardField.ISBN, "9783161484101");
        assertEquals(Map.of(), viewModel.findNewFetchableIdentifiers(entry));
    }

    @Test
    void findNewFetchableIdentifiersIgnoresInvalidEprint() {
        BibEntry entry = new BibEntry().withField(StandardField.EPRINT, "not-an-arxiv-id");
        assertEquals(Map.of(), viewModel.findNewFetchableIdentifiers(entry));
    }

    @Test
    void findNewFetchableIdentifiersNormalizesIssnDashes() {
        BibEntry hyphenated = new BibEntry().withField(StandardField.ISSN, "0378-5955");
        BibEntry plain = new BibEntry().withField(StandardField.ISSN, "03785955");
        viewModel.findNewFetchableIdentifiers(hyphenated);
        assertEquals(Map.of(), viewModel.findNewFetchableIdentifiers(plain));
    }

    @Test
    void findNewFetchableIdentifiersIgnoresInvalidIssnChecksum() {
        BibEntry entry = new BibEntry().withField(StandardField.ISSN, "0378-5954");
        assertEquals(Map.of(), viewModel.findNewFetchableIdentifiers(entry));
    }
}
