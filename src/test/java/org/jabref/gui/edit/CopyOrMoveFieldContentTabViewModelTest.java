package org.jabref.gui.edit;

import java.util.List;
import java.util.Optional;

import org.jabref.gui.edit.automaticfiededitor.copyormovecontent.CopyOrMoveFieldContentTabViewModel;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CopyOrMoveFieldContentTabViewModelTest {
    CopyOrMoveFieldContentTabViewModel copyOrMoveFieldContentTabViewModel;
    BibEntry entryA;
    BibEntry entryB;

    BibDatabase bibDatabase;

    @BeforeEach
    void setup() {
        entryA = new BibEntry(BibEntry.DEFAULT_TYPE)
                .withField(StandardField.YEAR, "2015")
                .withField(StandardField.DATE, "2014");

        entryB = new BibEntry(BibEntry.DEFAULT_TYPE)
                .withField(StandardField.DATE, "1998");
        bibDatabase = new BibDatabase();
        copyOrMoveFieldContentTabViewModel = newTwoFieldsViewModel(entryA, entryB);
    }

    @Test
    void copyValueDoesNotCopyBlankValues() {
        CopyOrMoveFieldContentTabViewModel copyOrMoveFieldContentTabViewModel = newTwoFieldsViewModel(entryA, entryB);

        copyOrMoveFieldContentTabViewModel.fromFieldProperty().set(StandardField.YEAR);
        copyOrMoveFieldContentTabViewModel.toFieldProperty().set(StandardField.DATE);
        copyOrMoveFieldContentTabViewModel.overwriteFieldContentProperty().set(true);
        copyOrMoveFieldContentTabViewModel.copyValue();

        assertEquals(Optional.of("2015"), entryA.getField(StandardField.DATE), "YEAR field is not copied correctly to the DATE field");
        assertEquals(Optional.of("2015"), entryA.getField(StandardField.YEAR), "YEAR field should not have changed");
        assertEquals(Optional.of("1998"), entryB.getField(StandardField.DATE), "DATE field should not have changed because the YEAR field is blank e.g it doesn't exist");
    }

    @Test
    void swapValuesShouldNotSwapFieldValuesIfOneOfTheValuesIsBlank() {
        copyOrMoveFieldContentTabViewModel.fromFieldProperty().set(StandardField.YEAR);
        copyOrMoveFieldContentTabViewModel.toFieldProperty().set(StandardField.DATE);
        copyOrMoveFieldContentTabViewModel.overwriteFieldContentProperty().set(true);

        copyOrMoveFieldContentTabViewModel.swapValues();

        assertEquals(Optional.of("1998"), entryB.getField(StandardField.DATE));
        assertEquals(Optional.empty(), entryB.getField(StandardField.YEAR));
    }

    @Test
    void swapValuesShouldSwapFieldValuesIfBothValuesAreNotBlank() {
        copyOrMoveFieldContentTabViewModel.fromFieldProperty().set(StandardField.YEAR);
        copyOrMoveFieldContentTabViewModel.toFieldProperty().set(StandardField.DATE);
        copyOrMoveFieldContentTabViewModel.overwriteFieldContentProperty().set(true);

        copyOrMoveFieldContentTabViewModel.swapValues();

        assertEquals(List.of(Optional.of("2014"), Optional.of("2015")),
                     List.of(entryA.getField(StandardField.YEAR), entryA.getField(StandardField.DATE)),
                     "YEAR and DATE values didn't swap");
    }

    @Test
    void moveValueShouldNotMoveValueIfToFieldIsNotBlankAndOverwriteIsNotEnabled() {
        copyOrMoveFieldContentTabViewModel.fromFieldProperty().set(StandardField.YEAR);
        copyOrMoveFieldContentTabViewModel.toFieldProperty().set(StandardField.DATE);
        copyOrMoveFieldContentTabViewModel.overwriteFieldContentProperty().set(false);

        copyOrMoveFieldContentTabViewModel.moveValue();

        assertEquals(Optional.of("2015"), entryA.getField(StandardField.YEAR));
        assertEquals(Optional.of("2014"), entryA.getField(StandardField.DATE));
    }

    @Test
    void moveValueShouldMoveValueIfOverwriteIsEnabled() {
        copyOrMoveFieldContentTabViewModel.fromFieldProperty().set(StandardField.DATE);
        copyOrMoveFieldContentTabViewModel.toFieldProperty().set(StandardField.YEAR);
        copyOrMoveFieldContentTabViewModel.overwriteFieldContentProperty().set(true);

        copyOrMoveFieldContentTabViewModel.moveValue();

        assertEquals(Optional.of("1998"), entryB.getField(StandardField.YEAR));
        assertEquals(Optional.empty(), entryB.getField(StandardField.DATE));
    }

    private CopyOrMoveFieldContentTabViewModel newTwoFieldsViewModel(BibEntry... selectedEntries) {
        return new CopyOrMoveFieldContentTabViewModel(List.of(selectedEntries), bibDatabase, new NamedCompound(""));
    }
}
