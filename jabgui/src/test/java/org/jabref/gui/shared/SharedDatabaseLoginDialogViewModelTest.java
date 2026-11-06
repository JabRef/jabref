package org.jabref.gui.shared;

import java.util.List;
import java.util.Optional;
import java.util.prefs.BackingStoreException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.preferences.LastFilesOpenedPreferences;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// The stored connections are mocks and the keyring is reported unavailable, so neither the user's preferences
/// nor the system keyring are touched.
class SharedDatabaseLoginDialogViewModelTest {

    private final SharedDatabasePreferences lastUsedConnection = mock(SharedDatabasePreferences.class);
    private final SharedDatabasePreferences storedConnection = mock(SharedDatabasePreferences.class);
    /// The same database, stored under a second identifier by a version that did not reuse connections yet
    private final SharedDatabasePreferences duplicateConnection = mock(SharedDatabasePreferences.class);
    private final LibraryTabContainer tabContainer = mock(LibraryTabContainer.class);
    private final ObservableList<LibraryTab> libraryTabs = FXCollections.observableArrayList();
    private final DialogService dialogService = mock(DialogService.class);
    private final ObservableList<String> lastSharedDatabasesOpened = FXCollections.observableArrayList("stored", "duplicate", "unrelated");

    private SharedDatabaseLoginDialogViewModel viewModel;

    @BeforeEach
    void setUp() throws BackingStoreException {
        when(lastUsedConnection.listSavedIds()).thenReturn(List.of("stored"));
        when(lastUsedConnection.forConnection("stored")).thenReturn(storedConnection);
        when(lastUsedConnection.forConnection("duplicate")).thenReturn(duplicateConnection);
        when(storedConnection.getDisplayLabel()).thenReturn("alice@localhost:5432/jabref");
        when(storedConnection.getHost()).thenReturn(Optional.of("localhost"));
        when(storedConnection.getUser()).thenReturn(Optional.of("alice"));
        when(storedConnection.listIdsOfSameDatabase()).thenReturn(List.of("stored", "duplicate"));
        when(storedConnection.remove()).thenReturn(true);
        when(duplicateConnection.remove()).thenReturn(true);
        when(tabContainer.getLibraryTabs()).thenReturn(libraryTabs);

        LastFilesOpenedPreferences lastFilesOpenedPreferences = mock(LastFilesOpenedPreferences.class);
        when(lastFilesOpenedPreferences.getLastSharedDatabasesOpened()).thenReturn(lastSharedDatabasesOpened);
        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getLastFilesOpenedPreferences()).thenReturn(lastFilesOpenedPreferences);

        viewModel = new SharedDatabaseLoginDialogViewModel(
                tabContainer,
                dialogService,
                preferences,
                lastUsedConnection,
                () -> false,
                mock(AiService.class),
                mock(StateManager.class),
                mock(BibEntryTypesManager.class),
                mock(FileUpdateMonitor.class),
                mock(ClipBoardManager.class),
                mock(TaskExecutor.class),
                mock(JournalAbbreviationRepository.class),
                mock(GitHandlerRegistry.class));
        viewModel.selectedSavedConnectionProperty().set(viewModel.getSavedConnections().getFirst());
    }

    private void openLibraryOf(String sharedDatabaseId) {
        BibDatabaseContext context = new BibDatabaseContext();
        context.getDatabase().setSharedDatabaseID(sharedDatabaseId);
        LibraryTab tab = mock(LibraryTab.class);
        when(tab.getBibDatabaseContext()).thenReturn(context);
        libraryTabs.add(tab);
    }

    @Test
    void selectingSavedConnectionFillsTheForm() {
        assertEquals("localhost", viewModel.hostProperty().get());
    }

    // [utest->req~shared-database.saved-connections-deletable~1]
    @Test
    void removingDeletesTheConnectionTogetherWithItsDuplicates() throws BackingStoreException {
        viewModel.removeSavedConnection();

        verify(storedConnection).remove();
        verify(duplicateConnection).remove();
        assertEquals(List.of(), viewModel.getSavedConnections());
    }

    // [utest->req~shared-database.saved-connections-deletable~1]
    @Test
    void removedConnectionIsNotReopenedOnStartup() {
        viewModel.removeSavedConnection();

        assertEquals(List.of("unrelated"), lastSharedDatabasesOpened);
    }

    /// Otherwise "Connect" would store the removed connection again right away
    @Test
    void removingClearsTheSelectionAndTheForm() {
        viewModel.removeSavedConnection();

        assertNull(viewModel.selectedSavedConnectionProperty().get());
        assertEquals("", viewModel.hostProperty().get());
        assertEquals("", viewModel.userProperty().get());
    }

    // [utest->req~shared-database.saved-connections-deletable~1]
    @Test
    void removingIsRefusedWhileTheLibraryOfADuplicateIsOpen() throws BackingStoreException {
        openLibraryOf("duplicate");

        viewModel.removeSavedConnection();

        verify(dialogService).showWarningDialogAndWait(anyString(), anyString());
        verify(storedConnection, never()).remove();
        verify(duplicateConnection, never()).remove();
        assertEquals(List.of("stored", "duplicate", "unrelated"), lastSharedDatabasesOpened);
        assertEquals(1, viewModel.getSavedConnections().size());
    }

    @Test
    void removingTheLastUsedConnectionClearsItsPrefill() throws BackingStoreException {
        when(lastUsedConnection.addressesSameDatabaseAs(storedConnection)).thenReturn(true);

        viewModel.removeSavedConnection();

        verify(lastUsedConnection).clear();
    }

    @Test
    void removingAnotherConnectionKeepsTheLastUsedPrefill() throws BackingStoreException {
        viewModel.removeSavedConnection();

        verify(lastUsedConnection, never()).clear();
    }
}
