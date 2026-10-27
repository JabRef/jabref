package org.jabref.gui.mergeentries.threewaymerge;

import java.util.List;
import java.util.Optional;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class MergeTwoEntriesActionTest {

    @Test
    void selectsMergedEntryInMatchingActiveTab() {
        StateManager stateManager = mock(StateManager.class);
        UndoManager undoManager = mock(UndoManager.class);
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        LibraryTab activeTab = mock(LibraryTab.class);
        BibEntry mergedEntry = new BibEntry();
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.ofNullable(activeTab));
        when(activeTab.getBibDatabaseContext()).thenReturn(databaseContext);
        when(undoManager.addEdit(anyString(), any())).thenReturn(true);

        MergeTwoEntriesAction action = new MergeTwoEntriesAction(
                new EntriesMergeResult(new BibEntry(), new BibEntry(), new BibEntry(), new BibEntry(), mergedEntry),
                stateManager,
                undoManager);
        action.execute();

        verify(stateManager).setSelectedEntries(List.of(mergedEntry));
        verify(activeTab).clearAndSelect(mergedEntry);
    }
}
