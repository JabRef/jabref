package org.jabref.gui.edit;

import java.util.List;
import java.util.Optional;

import javafx.collections.ObservableList;

import org.jabref.gui.StateManager;
import org.jabref.gui.edit.automaticfiededitor.clearfieldcontent.ClearFieldContentViewModel;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ClearFieldContentViewModelTest {
    ClearFieldContentViewModel clearFieldContentViewModel;

    BibEntry entryA;
    BibEntry entryB;

    ObservableList<Field> fieldsA;
    ObservableList<Field> fieldsB;
    ObservableList<Field> fields;

    BibDatabase bibDatabase;

    StateManager stateManager = mock(StateManager.class);

    @BeforeEach
    void setUp() {
        entryA = new BibEntry(BibEntry.DEFAULT_TYPE)
                .withField(StandardField.YEAR, "2015")
                .withField(StandardField.DATE, "2014")
                .withField(StandardField.AUTHOR, "Doe")
                .withField(StandardField.TITLE, "Some title")
                .withField(StandardField.JOURNAL, "Some journal");

        entryB = new BibEntry(BibEntry.DEFAULT_TYPE)
                .withField(StandardField.YEAR, "2015")
                .withField(StandardField.DATE, "2014")
                .withField(StandardField.ADDRESS, "Some address");

        bibDatabase = new BibDatabase();

        bibDatabase.insertEntry(entryA);
        bibDatabase.insertEntry(entryB);

        clearFieldContentViewModel = new ClearFieldContentViewModel(bibDatabase, List.of(entryA, entryB), stateManager);
    }

    @Test
    void getSetFieldsShouldReturnOnlyFieldsOfSelectedEntriesThatAreNonEmpty() {
        stateManager.setSelectedEntries(List.of(entryA, entryB));
        fields = clearFieldContentViewModel.getSetFields();
        assertEquals(6, fields.size());
    }

    @Test
    void getSetFieldsShouldHaveSameSizeAsRealSetFields() {
        fieldsA = clearFieldContentViewModel.getSetFields();

        int sizeOfFieldsA = fieldsA.size();

        assertEquals(5, sizeOfFieldsA);
    }

    @Test
    void clearSelectedFieldShouldClearFieldContent() {
        clearFieldContentViewModel.selectedFieldProperty().set(StandardField.YEAR);
        clearFieldContentViewModel.clearSelectedField();

        assertEquals(Optional.empty(), entryA.getField(StandardField.YEAR));
    }

    @Test
    void clearSelectedFieldShouldDoNothingWhenFieldDoesntExistOrIsEmpty() {
        clearFieldContentViewModel.selectedFieldProperty().set(StandardField.FILE);
        clearFieldContentViewModel.clearSelectedField();

        assertEquals(Optional.empty(), entryA.getField(StandardField.FILE));
    }
}
