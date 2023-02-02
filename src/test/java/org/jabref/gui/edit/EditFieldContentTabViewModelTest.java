package org.jabref.gui.edit;

import java.util.List;
import java.util.Optional;

import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.editfieldcontent.EditFieldContentViewModel;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

public class EditFieldContentTabViewModelTest {
    EditFieldContentViewModel editFieldContentViewModel;
    BibEntry entryA;
    BibEntry entryB;

    BibDatabase bibDatabase;

    StateManager stateManager = mock(StateManager.class);

    @BeforeEach
    void setup() {
        entryA = new BibEntry(BibEntry.DEFAULT_TYPE)
                .withField(StandardField.YEAR, "2015")
                .withField(StandardField.DATE, "2014");

        entryB = new BibEntry(BibEntry.DEFAULT_TYPE)
                .withField(StandardField.DATE, "1998")
                .withField(StandardField.YEAR, "");

        bibDatabase = new BibDatabase();
        editFieldContentViewModel = new EditFieldContentViewModel(bibDatabase, List.of(entryA, entryB), stateManager);
    }

    @Test
    void clearSelectedFieldShouldClearFieldContentEvenWhenOverwriteFieldContentIsNotEnabled() {
        editFieldContentViewModel.selectedFieldProperty().set(StandardField.YEAR);
        editFieldContentViewModel.overwriteFieldContentProperty().set(false);
        editFieldContentViewModel.clearSelectedField();

        assertEquals(Optional.empty(), entryA.getField(StandardField.YEAR));
    }

    @Test
    void clearSelectedFieldShouldDoNothingWhenFieldDoesntExistOrIsEmpty() {
        editFieldContentViewModel.selectedFieldProperty().set(StandardField.FILE);
        editFieldContentViewModel.clearSelectedField();

        assertEquals(Optional.empty(), entryA.getField(StandardField.FILE));
    }

    @Test
    void setFieldValueShouldNotDoAnythingIfOverwriteFieldContentIsNotEnabled() {
        editFieldContentViewModel.overwriteFieldContentProperty().set(false);
        editFieldContentViewModel.selectedFieldProperty().set(StandardField.YEAR);
        editFieldContentViewModel.fieldValueProperty().set("2001");
        editFieldContentViewModel.setFieldValue();

        assertEquals(Optional.of("2015"), entryA.getField(StandardField.YEAR));
    }

    @Test
    void setFieldValueShouldSetFieldValueIfOverwriteFieldContentIsEnabled() {
        editFieldContentViewModel.overwriteFieldContentProperty().set(true);
        editFieldContentViewModel.selectedFieldProperty().set(StandardField.YEAR);
        editFieldContentViewModel.fieldValueProperty().set("2001");
        editFieldContentViewModel.setFieldValue();

        assertEquals(Optional.of("2001"), entryA.getField(StandardField.YEAR));
    }

    @Test
    void setFieldValueShouldSetFieldValueIfFieldContentIsEmpty() {
        editFieldContentViewModel.overwriteFieldContentProperty().set(false);
        editFieldContentViewModel.selectedFieldProperty().set(StandardField.YEAR);
        editFieldContentViewModel.fieldValueProperty().set("2001");
        editFieldContentViewModel.setFieldValue();

        assertEquals(Optional.of("2001"), entryB.getField(StandardField.YEAR));
    }

    @Test
    void appendToFieldValueShouldDoNothingIfOverwriteFieldContentIsNotEnabled() {
        editFieldContentViewModel.overwriteFieldContentProperty().set(false);
        editFieldContentViewModel.selectedFieldProperty().set(StandardField.YEAR);
        editFieldContentViewModel.fieldValueProperty().set("0");
        editFieldContentViewModel.appendToFieldValue();

        assertEquals(Optional.of("2015"), entryA.getField(StandardField.YEAR));
    }

    @Test
    void appendToFieldValueShouldAppendFieldValueIfOverwriteFieldContentIsEnabled() {
        editFieldContentViewModel.overwriteFieldContentProperty().set(true);
        editFieldContentViewModel.selectedFieldProperty().set(StandardField.YEAR);
        editFieldContentViewModel.fieldValueProperty().set("0");
        editFieldContentViewModel.appendToFieldValue();

        assertEquals(Optional.of("20150"), entryA.getField(StandardField.YEAR));
    }

    @Test
    void getAllFieldsShouldNeverBeEmpty() {
        assertNotEquals(0, editFieldContentViewModel.getAllFields().size());
    }

    @Test
    void getSelectedFieldShouldHaveADefaultValue() {
        assertNotEquals(null, editFieldContentViewModel.getSelectedField());
    }
}
