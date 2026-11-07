package org.jabref.gui.shared;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
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
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
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
    private final GuiPreferences preferences = mock(GuiPreferences.class);
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
        when(preferences.getLastFilesOpenedPreferences()).thenReturn(lastFilesOpenedPreferences);

        viewModel = createViewModel(() -> false, mock(TaskExecutor.class));
        viewModel.selectedSavedConnectionProperty().set(viewModel.getSavedConnections().getFirst());
    }

    private SharedDatabaseLoginDialogViewModel createViewModel(BooleanSupplier keyringAvailability, TaskExecutor taskExecutor) {
        return new SharedDatabaseLoginDialogViewModel(
                tabContainer,
                dialogService,
                preferences,
                lastUsedConnection,
                keyringAvailability,
                mock(AiService.class),
                mock(StateManager.class),
                mock(BibEntryTypesManager.class),
                mock(FileUpdateMonitor.class),
                mock(ClipBoardManager.class),
                taskExecutor,
                mock(JournalAbbreviationRepository.class),
                mock(GitHandlerRegistry.class));
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

    /// A task executor that never runs the task keeps the keyring busy
    @Test
    void connectingWaitsWhileTheStoredPasswordIsRead() {
        when(storedConnection.getRememberPassword()).thenReturn(true);
        SharedDatabaseLoginDialogViewModel keyringViewModel = createViewModel(() -> true, mock(TaskExecutor.class));

        keyringViewModel.selectedSavedConnectionProperty().set(keyringViewModel.getSavedConnections().getFirst());

        assertTrue(keyringViewModel.passwordLoadingProperty().get());
    }

    @Test
    void storedPasswordIsFilledInOnceRead() {
        when(storedConnection.getRememberPassword()).thenReturn(true);
        when(storedConnection.getPassword()).thenReturn(Optional.of("secret"));
        SharedDatabaseLoginDialogViewModel keyringViewModel = createViewModel(() -> true, new CurrentThreadTaskExecutor());

        keyringViewModel.selectedSavedConnectionProperty().set(keyringViewModel.getSavedConnections().getFirst());

        assertEquals("secret", keyringViewModel.passwordProperty().get());
        assertFalse(keyringViewModel.passwordLoadingProperty().get());
    }

    /// Selects the stored connection in a dialog whose keyring read only finishes when the returned task is completed
    private BackgroundTask<Optional<String>> selectWithPendingPasswordRead(SharedDatabaseLoginDialogViewModel keyringViewModel, TaskExecutor taskExecutor) {
        keyringViewModel.selectedSavedConnectionProperty().set(keyringViewModel.getSavedConnections().getFirst());
        ArgumentCaptor<BackgroundTask<Optional<String>>> passwordRead = ArgumentCaptor.captor();
        verify(taskExecutor, atLeastOnce()).execute(passwordRead.capture());
        return passwordRead.getValue();
    }

    private static void complete(BackgroundTask<Optional<String>> passwordRead) throws Exception {
        passwordRead.getOnSuccess().accept(passwordRead.call());
    }

    @Test
    void passwordReadFinishingAfterAnEditIsDropped() throws Exception {
        when(storedConnection.getRememberPassword()).thenReturn(true);
        when(storedConnection.getPassword()).thenReturn(Optional.of("secret"));
        TaskExecutor taskExecutor = mock(TaskExecutor.class);
        SharedDatabaseLoginDialogViewModel keyringViewModel = createViewModel(() -> true, taskExecutor);
        BackgroundTask<Optional<String>> passwordRead = selectWithPendingPasswordRead(keyringViewModel, taskExecutor);

        keyringViewModel.hostProperty().set("other.example.org");
        complete(passwordRead);

        assertEquals("", keyringViewModel.passwordProperty().get());
    }

    @Test
    void typingPasswordAllowsConnectingDuringASlowRead() {
        when(storedConnection.getRememberPassword()).thenReturn(true);
        TaskExecutor taskExecutor = mock(TaskExecutor.class);
        SharedDatabaseLoginDialogViewModel keyringViewModel = createViewModel(() -> true, taskExecutor);
        selectWithPendingPasswordRead(keyringViewModel, taskExecutor);

        keyringViewModel.passwordProperty().set("typed");

        assertFalse(keyringViewModel.passwordLoadingProperty().get());
    }

    @Test
    void passwordReadFinishingAfterARemovalIsDropped() throws Exception {
        when(storedConnection.getRememberPassword()).thenReturn(true);
        when(storedConnection.getPassword()).thenReturn(Optional.of("secret"));
        TaskExecutor taskExecutor = mock(TaskExecutor.class);
        SharedDatabaseLoginDialogViewModel keyringViewModel = createViewModel(() -> true, taskExecutor);
        BackgroundTask<Optional<String>> passwordRead = selectWithPendingPasswordRead(keyringViewModel, taskExecutor);

        keyringViewModel.removeSavedConnection();
        complete(passwordRead);

        assertEquals("", keyringViewModel.passwordProperty().get());
    }

    @Test
    void selectingConnectionWithoutStoredPortUsesTheDefaultPort() {
        viewModel.selectedSavedConnectionProperty().set(null);
        viewModel.portProperty().set("6543");

        viewModel.selectedSavedConnectionProperty().set(viewModel.getSavedConnections().getFirst());

        assertEquals("5432", viewModel.portProperty().get());
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

    @Test
    void removingResetsTheConnectionOptions() {
        viewModel.useSSLProperty().set(true);
        viewModel.rememberPasswordProperty().set(true);
        viewModel.autosaveProperty().set(true);
        viewModel.folderProperty().set("/tmp/library.bib");

        viewModel.removeSavedConnection();

        assertEquals(List.of(false, false, false, ""), List.of(
                viewModel.useSSLProperty().get(),
                viewModel.rememberPasswordProperty().get(),
                viewModel.autosaveProperty().get(),
                viewModel.folderProperty().get()));
    }

    /// The settings of a removed node can no longer be read
    @Test
    void lastUsedConnectionIsComparedBeforeTheConnectionIsRemoved() throws BackingStoreException {
        viewModel.removeSavedConnection();

        InOrder inOrder = inOrder(lastUsedConnection, storedConnection);
        inOrder.verify(lastUsedConnection).addressesSameDatabaseAs(storedConnection);
        inOrder.verify(storedConnection).remove();
    }

    // [utest->req~shared-database.saved-connections-deletable~1]
    @Test
    void removingIsRefusedWhileTheReconnectOfTheConnectionFailed() throws BackingStoreException {
        when(tabContainer.getUnconnectedSharedDatabaseIds()).thenReturn(List.of("stored"));

        viewModel.removeSavedConnection();

        verify(dialogService).showWarningDialogAndWait(anyString(), anyString());
        verify(storedConnection, never()).remove();
        assertEquals(1, viewModel.getSavedConnections().size());
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

    /// The listed identifier may be gone while a duplicate is still stored
    @Test
    void failedRemovalOfADuplicateListsWhatIsStillStored() throws BackingStoreException {
        when(duplicateConnection.remove()).thenThrow(new BackingStoreException("Disk full"));
        when(duplicateConnection.getDisplayLabel()).thenReturn("alice@localhost:5432/jabref");
        when(lastUsedConnection.listSavedIds()).thenReturn(List.of("duplicate"));

        viewModel.removeSavedConnection();

        assertEquals(List.of("duplicate"), viewModel.getSavedConnections().stream().map(SharedDatabaseLoginDialogViewModel.SavedConnection::id).toList());
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
