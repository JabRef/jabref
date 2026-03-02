package org.jabref.gui.mergeentries.multiwaymerge;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void updateFieldsDoesNotOverwriteWhenNoComparatorExists() {
        // For fields like TITLE, no plausibility comparator exists → first source wins
        BibEntry leftEntry = new BibEntry().withField(StandardField.TITLE, "First Title");
        BibEntry rightEntry = new BibEntry().withField(StandardField.TITLE, "Second Title");

        viewModel.updateFields(leftEntry);
        viewModel.updateFields(rightEntry);

        assertEquals("First Title", viewModel.mergedEntryProperty().get().getField(StandardField.TITLE).orElse(""));
    }

    @Test
    void updateFieldsSetsFieldWhenNotYetPresent() {
        BibEntry source = new BibEntry().withField(StandardField.YEAR, "2015");
        viewModel.updateFields(source);
        assertEquals("2015", viewModel.mergedEntryProperty().get().getField(StandardField.YEAR).orElse(""));
    }

    @Test
    void updateFieldsPrefersMoreCompleteAuthorList() {
        BibEntry leftEntry = new BibEntry().withField(StandardField.AUTHOR, "Wang, J.");
        BibEntry rightEntry = new BibEntry().withField(StandardField.AUTHOR, "Wang, Jun and Smith, Alice");

        viewModel.updateFields(leftEntry);
        viewModel.updateFields(rightEntry);

        assertEquals("Wang, Jun and Smith, Alice", viewModel.mergedEntryProperty().get().getField(StandardField.AUTHOR).orElse(""));
    }

    @Test
    void updateFieldsPrefersBetterYearOverFirstSeen() {
        // malformed
        BibEntry leftEntry = new BibEntry().withField(StandardField.YEAR, "201");
        // valid
        BibEntry rightEntry = new BibEntry().withField(StandardField.YEAR, "2015");

        viewModel.updateFields(leftEntry);
        viewModel.updateFields(rightEntry);

        assertEquals("2015", viewModel.mergedEntryProperty().get().getField(StandardField.YEAR).orElse(""));
    }

    @Test
    void updateFieldsPrefersBibtexMonthFormat() {
        // plain text
        BibEntry leftEntry = new BibEntry().withField(StandardField.MONTH, "June");
        // BibTeX constant
        BibEntry rightEntry = new BibEntry().withField(StandardField.MONTH, "#jun#");

        viewModel.updateFields(leftEntry);
        viewModel.updateFields(rightEntry);

        assertEquals("#jun#", viewModel.mergedEntryProperty().get().getField(StandardField.MONTH).orElse(""));
    }

    @Test
    void updateFieldsPrefersMoreSpecificDate() {
        BibEntry leftEntry = new BibEntry().withField(StandardField.DATE, "2017-09");
        BibEntry rightEntry = new BibEntry().withField(StandardField.DATE, "2017-09-12");

        viewModel.updateFields(leftEntry);
        viewModel.updateFields(rightEntry);

        assertEquals("2017-09-12", viewModel.mergedEntryProperty().get().getField(StandardField.DATE).orElse(""));
    }

    @Test
    void updateFieldsPrefersAnyDateOverEmpty() {
        BibEntry leftEntry = new BibEntry();
        BibEntry rightEntry = new BibEntry().withField(StandardField.DATE, "2017-09-12");

        viewModel.updateFields(leftEntry);
        viewModel.updateFields(rightEntry);

        assertEquals("2017-09-12", viewModel.mergedEntryProperty().get().getField(StandardField.DATE).orElse(""));
    }

    @Test
    void updateFieldsPrefersValidDateOverMalformedDate() {
        BibEntry leftEntry = new BibEntry().withField(StandardField.DATE, "foo-bar-baz");
        BibEntry rightEntry = new BibEntry().withField(StandardField.DATE, "2017-09");

        viewModel.updateFields(leftEntry);
        viewModel.updateFields(rightEntry);

        assertEquals("2017-09", viewModel.mergedEntryProperty().get().getField(StandardField.DATE).orElse(""));
    }
}
