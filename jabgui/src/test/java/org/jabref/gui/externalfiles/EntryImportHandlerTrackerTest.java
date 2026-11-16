package org.jabref.gui.externalfiles;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void markKeptExistingCountsAsSkippedAndStaysOutOfImportedEntries() {
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.empty());
        BibEntry importedEntry = new BibEntry().withCitationKey("Imported");
        BibEntry keptEntry = new BibEntry().withCitationKey("Kept");

        EntryImportHandlerTracker tracker = new EntryImportHandlerTracker(stateManager, new BibDatabaseContext(), 2);
        tracker.markImported(importedEntry);
        tracker.markKeptExisting(keptEntry);

        assertEquals(1, tracker.getImportedCount());
        assertEquals(1, tracker.getSkippedCount());
        assertEquals(List.of(importedEntry), tracker.getImportedEntries());
        assertEquals(List.of(keptEntry), tracker.getKeptExistingEntries());
        verify(stateManager).setSelectedEntries(List.of(importedEntry));
    }

    @Test
    void markKeptExistingRunsOnFinishOnceWhenItIsTheLastEntry() {
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.empty());
        AtomicInteger finishCalls = new AtomicInteger();

        EntryImportHandlerTracker tracker = new EntryImportHandlerTracker(stateManager, new BibDatabaseContext(), 2);
        tracker.setOnFinish(finishCalls::incrementAndGet);
        tracker.markSkipped();
        assertEquals(0, finishCalls.get());

        tracker.markKeptExisting(new BibEntry().withCitationKey("Kept"));
        tracker.markSkipped();

        assertEquals(1, finishCalls.get());
    }
}
