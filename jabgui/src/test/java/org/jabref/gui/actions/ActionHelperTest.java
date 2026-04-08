package org.jabref.gui.actions;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.gui.StateManager;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActionHelperTest {

    @Test
    void hasLinkedFileForSelectedEntriesReturnsTrueWhenEntryHasLinkedFile() {
        BibEntry entry = new BibEntry().withFiles(List.of(new LinkedFile("", Path.of("paper.pdf"), "pdf")));
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(entry));

        assertTrue(ActionHelper.hasLinkedFileForSelectedEntries(stateManager).get());
    }

    @Test
    void hasLinkedFileForSelectedEntriesReturnsTrueWhenCrossrefResolvesToEntryWithFiles() {
        BibDatabase database = new BibDatabase();
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);

        BibEntry parentEntry = new BibEntry()
                .withCitationKey("parent-key")
                .withFiles(List.of(new LinkedFile("", Path.of("parent.pdf"), "pdf")));
        BibEntry childEntry = new BibEntry().withField(StandardField.CROSSREF, "parent-key");
        database.insertEntries(List.of(parentEntry, childEntry));

        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(childEntry));
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));

        assertTrue(ActionHelper.hasLinkedFileForSelectedEntries(stateManager).get());
    }

    @Test
    void hasLinkedFileForSelectedEntriesReturnsFalseWhenActiveDatabaseMissing() {
        BibEntry entry = new BibEntry().withField(StandardField.CROSSREF, "parent-key");
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(entry));
        when(stateManager.getActiveDatabase()).thenReturn(Optional.empty());

        assertFalse(ActionHelper.hasLinkedFileForSelectedEntries(stateManager).get());
    }

    @Test
    void hasLinkedFileForSelectedEntriesReturnsFalseWhenCrossrefCannotBeResolved() {
        BibDatabase database = new BibDatabase();
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);
        BibEntry entry = new BibEntry().withField(StandardField.CROSSREF, "missing-parent");
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(entry));
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));

        assertFalse(ActionHelper.hasLinkedFileForSelectedEntries(stateManager).get());
    }

    @Test
    void hasLinkedFileForSelectedEntriesReturnsFalseWhenEntryHasNoLinkedFileOrCrossref() {
        BibEntry entry = new BibEntry();
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(entry));
        when(stateManager.getActiveDatabase()).thenReturn(Optional.empty());

        assertFalse(ActionHelper.hasLinkedFileForSelectedEntries(stateManager).get());
    }
}
