package org.jabref.gui.git;

import java.util.List;
import java.util.Optional;

import javafx.scene.control.Dialog;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.exporter.SaveDatabaseAction.SaveDatabaseMode;
import org.jabref.gui.exporter.SaveDatabaseAction.SaveResult;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.entry.BibEntryTypesManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GitCommitActionTest {

    private final DialogService dialogService = mock(DialogService.class);
    private final StateManager stateManager = mock(StateManager.class);
    private final GuiPreferences preferences = mock(GuiPreferences.class);
    private final LibraryTab libraryTab = mock(LibraryTab.class);
    private final LibraryPreferences libraryPreferences = mock(LibraryPreferences.class);

    private GitCommitAction gitCommitAction;

    @BeforeEach
    void setup() {
        when(stateManager.activeDatabaseProperty()).thenReturn(OptionalObjectProperty.empty());
        // Without an active database the Git status check stops at "nothing to commit", so no dialog is built
        when(stateManager.getActiveDatabase()).thenReturn(Optional.empty());
        when(preferences.getLibraryPreferences()).thenReturn(libraryPreferences);
        when(libraryPreferences.shouldAutoSave()).thenReturn(true);

        gitCommitAction = new GitCommitAction(
                () -> libraryTab,
                dialogService,
                stateManager,
                preferences,
                mock(BibEntryTypesManager.class),
                mock(JournalAbbreviationRepository.class));
    }

    @Test
    void modifiedLibraryIsSavedBeforeGitStatusIsChecked() {
        when(libraryTab.isModified()).thenReturn(true);

        try (MockedConstruction<SaveDatabaseAction> saveDatabaseAction = mockConstruction(SaveDatabaseAction.class,
                (mockedSave, _) -> when(mockedSave.save(SaveDatabaseMode.NORMAL)).thenReturn(SaveResult.SUCCESS))) {
            gitCommitAction.execute();

            SaveDatabaseAction save = saveDatabaseAction.constructed().getFirst();
            InOrder inOrder = inOrder(save, stateManager);
            inOrder.verify(save).save(SaveDatabaseMode.NORMAL);
            inOrder.verify(stateManager).getActiveDatabase();
        }
    }

    @Test
    void unmodifiedLibraryIsNotSaved() {
        when(libraryTab.isModified()).thenReturn(false);

        try (MockedConstruction<SaveDatabaseAction> saveDatabaseAction = mockConstruction(SaveDatabaseAction.class)) {
            gitCommitAction.execute();

            assertEquals(List.of(), saveDatabaseAction.constructed());
            verify(stateManager).getActiveDatabase();
        }
    }

    @Test
    void modifiedLibraryWithoutAutosaveIsNotSavedAndAbortsCommit() {
        when(libraryTab.isModified()).thenReturn(true);
        when(libraryPreferences.shouldAutoSave()).thenReturn(false);

        try (MockedConstruction<SaveDatabaseAction> saveDatabaseAction = mockConstruction(SaveDatabaseAction.class)) {
            gitCommitAction.execute();

            assertEquals(List.of(), saveDatabaseAction.constructed());
            verify(dialogService).showWarningDialogAndWait(any(), any());
            verify(stateManager, never()).getActiveDatabase();
        }
    }

    @ParameterizedTest
    @EnumSource(value = SaveResult.class, names = {"FAILURE", "ALREADY_SAVING"})
    void unsuccessfulSaveAbortsCommit(SaveResult saveResult) {
        when(libraryTab.isModified()).thenReturn(true);

        try (MockedConstruction<SaveDatabaseAction> saveDatabaseAction = mockConstruction(SaveDatabaseAction.class,
                (mockedSave, _) -> when(mockedSave.save(SaveDatabaseMode.NORMAL)).thenReturn(saveResult))) {
            gitCommitAction.execute();

            verify(saveDatabaseAction.constructed().getFirst()).save(SaveDatabaseMode.NORMAL);
            verify(stateManager, never()).getActiveDatabase();
            verify(dialogService, never()).showCustomDialogAndWait(any(Dialog.class));
        }
    }
}
