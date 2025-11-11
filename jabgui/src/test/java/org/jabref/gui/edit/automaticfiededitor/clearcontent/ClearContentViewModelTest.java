package org.jabref.gui.edit.automaticfiededitor.clearcontent;

import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.gui.StateManager;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClearContentViewModelTest {
    ClearContentViewModel clearContentViewModel;
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
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.AUTHOR, "Author");
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(entryA, entryB));

        bibDatabase = new BibDatabase();
        clearContentViewModel = new ClearContentViewModel(stateManager);
    }

    @Test
    void clearSelectedFieldShouldDoNothingWhenFieldDoesntExistOrIsEmpty() {
        clearContentViewModel.clearField(StandardField.YEAR);

        assertEquals(Optional.empty(), entryA.getField(StandardField.FILE));
    }

    @Test
    void clearExistingFieldShouldRemoveFieldFromEntry() {
        clearContentViewModel.clearField(StandardField.YEAR);

        assertEquals(Optional.empty(), entryA.getField(StandardField.YEAR));
    }

    @Test
    void clearNonExistingFieldShouldNotAffectEntry() {
        clearContentViewModel.clearField(StandardField.AUTHOR);

        assertEquals(Optional.of("2015"), entryA.getField(StandardField.YEAR));
        assertEquals(Optional.of("2014"), entryA.getField(StandardField.DATE));
    }

    @Test
    void clearEmptyFieldShouldNotAffectEntry() {
        clearContentViewModel.clearField(StandardField.DATE);

        assertEquals(Optional.of("2015"), entryA.getField(StandardField.YEAR));
        assertEquals(Optional.empty(), entryA.getField(StandardField.DATE));
    }

    @Test
    void clearFieldAffectsOnlyEntryAForNonExistingField() {
        clearContentViewModel.clearField(StandardField.FILE);

        assertEquals(Optional.of("2015"), entryA.getField(StandardField.YEAR));
        assertEquals(Optional.of("2020"), entryB.getField(StandardField.YEAR));
        assertEquals(Optional.of("Author"), entryB.getField(StandardField.AUTHOR));
    }
}
