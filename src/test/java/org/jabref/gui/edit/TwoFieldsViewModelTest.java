package org.jabref.gui.edit;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.edit.automaticfiededitor.twofields.TwoFieldsViewModel;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TwoFieldsViewModelTest {
    TwoFieldsViewModel twoFieldsViewModel;
    BibEntry entryA;
    BibEntry entryB;

    @BeforeEach
    void setup() {
        entryA = new BibEntry(BibEntry.DEFAULT_TYPE)
                .withField(StandardField.YEAR, "2015")
                .withField(StandardField.DATE, "2014");

        entryB = new BibEntry(BibEntry.DEFAULT_TYPE)
                .withField(StandardField.DATE, "1998");

        twoFieldsViewModel = newTwoFieldsViewModel(entryA, entryB);
    }

    @Test
    void copyValueDoesNotCopyBlankValues() {
        TwoFieldsViewModel twoFieldsViewModel = newTwoFieldsViewModel(entryA, entryB);

        twoFieldsViewModel.fromFieldProperty().set(StandardField.YEAR);
        twoFieldsViewModel.toFieldProperty().set(StandardField.DATE);
        twoFieldsViewModel.overwriteFieldContentProperty().set(true);
        twoFieldsViewModel.copyValue();

        assertEquals(Optional.of("2015"), entryA.getField(StandardField.DATE), "YEAR field is not copied correctly to the DATE field");
        assertEquals(Optional.of("2015"), entryA.getField(StandardField.YEAR), "YEAR field should not have changed");
        assertEquals(Optional.of("1998"), entryB.getField(StandardField.DATE), "DATE field should not have changed because the YEAR field is blank e.g it doesn't exist");
    }

    @Test
    void swapValuesShouldNotSwapFieldValuesIfOneOfTheValuesIsBlank() {
        twoFieldsViewModel.fromFieldProperty().set(StandardField.YEAR);
        twoFieldsViewModel.toFieldProperty().set(StandardField.DATE);
        twoFieldsViewModel.overwriteFieldContentProperty().set(true);

        twoFieldsViewModel.swapValues();

        assertEquals(Optional.of("1998"), entryB.getField(StandardField.DATE));
        assertEquals(Optional.empty(), entryB.getField(StandardField.YEAR));
    }

    @Test
    void swapValuesShouldSwapFieldValuesIfBothValuesAreNotBlank() {
        twoFieldsViewModel.fromFieldProperty().set(StandardField.YEAR);
        twoFieldsViewModel.toFieldProperty().set(StandardField.DATE);
        twoFieldsViewModel.overwriteFieldContentProperty().set(true);

        twoFieldsViewModel.swapValues();

        assertEquals(List.of(Optional.of("2014"), Optional.of("2015")),
                     List.of(entryA.getField(StandardField.YEAR), entryA.getField(StandardField.DATE)),
                     "YEAR and DATE values didn't swap");
    }

    @Test
    void moveValueShouldNotMoveValueIfToFieldIsNotBlankAndOverwriteIsNotEnabled() {
        twoFieldsViewModel.fromFieldProperty().set(StandardField.YEAR);
        twoFieldsViewModel.toFieldProperty().set(StandardField.DATE);
        twoFieldsViewModel.overwriteFieldContentProperty().set(false);

        twoFieldsViewModel.moveValue();

        assertEquals(Optional.of("2015"), entryA.getField(StandardField.YEAR));
        assertEquals(Optional.of("2014"), entryA.getField(StandardField.DATE));
    }

    @Test
    void moveValueShouldMoveValueIfOverwriteIsEnabled() {
        twoFieldsViewModel.fromFieldProperty().set(StandardField.DATE);
        twoFieldsViewModel.toFieldProperty().set(StandardField.YEAR);
        twoFieldsViewModel.overwriteFieldContentProperty().set(true);

        twoFieldsViewModel.moveValue();

        assertEquals(Optional.of("1998"), entryB.getField(StandardField.YEAR));
        assertEquals(Optional.empty(), entryB.getField(StandardField.DATE));
    }

    private TwoFieldsViewModel newTwoFieldsViewModel(BibEntry... selectedEntries) {
        return new TwoFieldsViewModel(List.of(selectedEntries), Collections.emptySet(), new NamedCompound(""));
    }
}
