package org.jabref.gui.externalfiles;

import java.util.List;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class EntryImportHandlerTrackerTest {

    @Test
    void selectsImportedEntriesWithoutCitationKeyInMatchingActiveTab() {
        StateManager stateManager = mock(StateManager.class);
        BibDatabaseContext targetDatabaseContext = new BibDatabaseContext();
        LibraryTab activeTab = mock(LibraryTab.class);
        BibEntry importedEntry = new BibEntry();
        when(activeTab.getBibDatabaseContext()).thenReturn(targetDatabaseContext);
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.ofNullable(activeTab));

        EntryImportHandlerTracker tracker = new EntryImportHandlerTracker(stateManager, targetDatabaseContext);
        tracker.markImported(importedEntry);

        assertTrue(importedEntry.getCitationKey().isEmpty());
        verify(stateManager).setSelectedEntries(List.of(importedEntry));
        verify(activeTab).clearAndSelect(List.of(importedEntry));
    }

    @Test
    void doesNotSelectImportedEntriesAfterSwitchingToAnotherLibrary() {
        StateManager stateManager = mock(StateManager.class);
        BibDatabaseContext targetDatabaseContext = new BibDatabaseContext();
        LibraryTab activeTab = mock(LibraryTab.class);
        BibEntry importedEntry = new BibEntry();
        when(activeTab.getBibDatabaseContext()).thenReturn(new BibDatabaseContext());
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.ofNullable(activeTab));

        EntryImportHandlerTracker tracker = new EntryImportHandlerTracker(stateManager, targetDatabaseContext);
        tracker.markImported(importedEntry);

        verify(stateManager).setSelectedEntries(List.of(importedEntry));
        verify(activeTab, never()).clearAndSelect(List.of(importedEntry));
    }
}
