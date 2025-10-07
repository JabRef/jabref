package org.jabref.gui.edit;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javafx.collections.FXCollections;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClearFieldContentViewModelTest {
    ClearFieldContentViewModel clearFieldContentViewModel;

    BibEntry entryA;
    BibEntry entryB;
    ObservableList<BibEntry> selectedEntries = FXCollections.observableArrayList();

    BibDatabase bibDatabase;

    StateManager stateManager;

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

        selectedEntries.addAll(entryA, entryB);

        stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(selectedEntries);

        clearFieldContentViewModel = new ClearFieldContentViewModel(bibDatabase, List.of(entryA, entryB), stateManager);
    }

    @Test
    void getSetFieldsShouldReturnOnlyFieldsOfSelectedEntriesThatAreNonEmpty() {
        assertEquals(6, clearFieldContentViewModel.getSetFields().size());
    }

    @Test
    void allSetFieldsShouldBeNonEmpty() {
        Set<Field> allSetFieldsEntryA = entryA.getFields();
        for (Field field : allSetFieldsEntryA) {
            Optional<String> valueOfField = entryA.getField(field);
            assertTrue(valueOfField.isPresent());
        }

        Set<Field> allSetFieldsEntryB = entryB.getFields();
        for (Field field : allSetFieldsEntryB) {
            Optional<String> valueOfField = entryB.getField(field);
            assertTrue(valueOfField.isPresent());
        }
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
