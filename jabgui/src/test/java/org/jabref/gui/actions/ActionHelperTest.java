package org.jabref.gui.actions;

import java.nio.file.Path;
import java.util.List;

import javafx.collections.FXCollections;

import org.jabref.gui.StateManager;
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
    void hasLinkedFileForSelectedEntriesReturnsTrueWhenEntryHasCrossref() {
        BibEntry entry = new BibEntry().withField(StandardField.CROSSREF, "parent-key");
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(entry));

        assertTrue(ActionHelper.hasLinkedFileForSelectedEntries(stateManager).get());
    }

    @Test
    void hasLinkedFileForSelectedEntriesReturnsFalseWhenEntryHasNoLinkedFileOrCrossref() {
        BibEntry entry = new BibEntry();
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(entry));

        assertFalse(ActionHelper.hasLinkedFileForSelectedEntries(stateManager).get());
    }
}
