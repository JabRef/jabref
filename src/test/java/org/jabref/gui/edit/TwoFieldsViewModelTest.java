package org.jabref.gui.edit;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.edit.automaticfiededitor.twofields.TwoFieldsViewModel;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TwoFieldsViewModelTest {

    @Test
    void copyValueDoesNotCopyBlankValues() {
        BibEntry entryA = new BibEntry(BibEntry.DEFAULT_TYPE)
                .withField(StandardField.YEAR, "2015")
                .withField(StandardField.DATE, "2014");

        BibEntry entryB = new BibEntry(BibEntry.DEFAULT_TYPE)
                .withField(StandardField.DATE, "1998");

        TwoFieldsViewModel twoFieldsViewModel = newTwoFieldsViewModel(entryA, entryB);

        twoFieldsViewModel.fromFieldProperty().set(StandardField.YEAR);
        twoFieldsViewModel.toFieldProperty().set(StandardField.DATE);
        twoFieldsViewModel.overwriteNonEmptyFieldsProperty().set(true);
        twoFieldsViewModel.copyValue();

        assertEquals(Optional.of("2015"), entryA.getField(StandardField.DATE), "YEAR field is not copied correctly to the DATE field");
        assertEquals(Optional.of("2015"), entryA.getField(StandardField.YEAR), "YEAR field should not have changed");
        assertEquals(Optional.of("1998"), entryB.getField(StandardField.DATE), "DATE field should not have changed because the YEAR field is blank e.g it doesn't exist");
    }

    private TwoFieldsViewModel newTwoFieldsViewModel(BibEntry...selectedEntries) {
        return new TwoFieldsViewModel(List.of(selectedEntries), Collections.emptySet(), new NamedCompound(""));
    }
}
