package org.jabref.gui.mergeentries.multiwaymerge;

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
}
