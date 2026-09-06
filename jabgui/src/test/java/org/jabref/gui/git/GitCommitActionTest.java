package org.jabref.gui.git;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.exporter.SaveDatabaseAction.SaveDatabaseMode;
import org.jabref.gui.exporter.SaveDatabaseAction.SaveResult;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GitCommitActionTest {

    @TempDir
    Path libraryDirectory;

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
                mock(JournalAbbreviationRepository.class),
                new CurrentThreadTaskExecutor(),
                new GitHandlerRegistry(mock(GitPreferences.class)));
    }

    @Test
    void modifiedLibraryIsSavedBeforeGitStatusIsChecked() {
        when(libraryTab.isModified()).thenReturn(true);

        try (MockedConstruction<SaveDatabaseAction> saveDatabaseAction = mockConstruction(SaveDatabaseAction.class,
                (mockedSave, _) -> when(mockedSave.saveWithoutGitAutoCommit(SaveDatabaseMode.NORMAL)).thenReturn(SaveResult.SUCCESS))) {
            gitCommitAction.execute();

            SaveDatabaseAction save = saveDatabaseAction.constructed().getFirst();
            InOrder inOrder = inOrder(save, stateManager);
            inOrder.verify(save).saveWithoutGitAutoCommit(SaveDatabaseMode.NORMAL);
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

    // [utest->req~git.commit.unsaved-changes~1]
    @Test
    void cancellingLeavesTheLibraryUnsavedAndAbortsCommit() {
        when(libraryTab.isModified()).thenReturn(true);
        when(libraryPreferences.shouldAutoSave()).thenReturn(false);
        when(dialogService.showCustomButtonDialogAndWait(any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        try (MockedConstruction<SaveDatabaseAction> saveDatabaseAction = mockConstruction(SaveDatabaseAction.class)) {
            gitCommitAction.execute();

            assertEquals(List.of(), saveDatabaseAction.constructed());
            verify(stateManager, never()).getActiveDatabase();
        }
    }

    // [utest->req~git.commit.unsaved-changes~1]
    @Test
    void committingWithoutSavingLeavesTheLibraryUnsaved() {
        when(libraryTab.isModified()).thenReturn(true);
        when(libraryPreferences.shouldAutoSave()).thenReturn(false);
        clickButton(Localization.lang("Commit without saving"));

        try (MockedConstruction<SaveDatabaseAction> saveDatabaseAction = mockConstruction(SaveDatabaseAction.class)) {
            gitCommitAction.execute();

            assertEquals(List.of(), saveDatabaseAction.constructed());
            verify(stateManager).getActiveDatabase();
        }
    }

    // [utest->req~git.commit.unsaved-changes~1]
    @Test
    void savingAndCommittingWritesTheLibraryFirst() {
        when(libraryTab.isModified()).thenReturn(true);
        when(libraryPreferences.shouldAutoSave()).thenReturn(false);
        clickButton(Localization.lang("Save and commit"));

        try (MockedConstruction<SaveDatabaseAction> saveDatabaseAction = mockConstruction(SaveDatabaseAction.class,
                (mockedSave, _) -> when(mockedSave.saveWithoutGitAutoCommit(SaveDatabaseMode.NORMAL)).thenReturn(SaveResult.SUCCESS))) {
            gitCommitAction.execute();

            verify(saveDatabaseAction.constructed().getFirst()).saveWithoutGitAutoCommit(SaveDatabaseMode.NORMAL);
            verify(stateManager).getActiveDatabase();
        }
    }

    // [utest->req~git.commit.unsaved-changes~1]
    @Test
    void unsuccessfulSaveAfterChoosingToSaveAbortsCommit() {
        when(libraryTab.isModified()).thenReturn(true);
        when(libraryPreferences.shouldAutoSave()).thenReturn(false);
        clickButton(Localization.lang("Save and commit"));

        try (MockedConstruction<SaveDatabaseAction> saveDatabaseAction = mockConstruction(SaveDatabaseAction.class,
                (mockedSave, _) -> when(mockedSave.saveWithoutGitAutoCommit(SaveDatabaseMode.NORMAL)).thenReturn(SaveResult.FAILURE))) {
            gitCommitAction.execute();

            verify(stateManager, never()).getActiveDatabase();
        }
    }

    @ParameterizedTest
    @EnumSource(value = SaveResult.class, names = {"FAILURE", "ALREADY_SAVING"})
    void unsuccessfulSaveAbortsCommit(SaveResult saveResult) {
        when(libraryTab.isModified()).thenReturn(true);

        try (MockedConstruction<SaveDatabaseAction> saveDatabaseAction = mockConstruction(SaveDatabaseAction.class,
                (mockedSave, _) -> when(mockedSave.saveWithoutGitAutoCommit(SaveDatabaseMode.NORMAL)).thenReturn(saveResult))) {
            gitCommitAction.execute();

            verify(saveDatabaseAction.constructed().getFirst()).saveWithoutGitAutoCommit(SaveDatabaseMode.NORMAL);
            verify(stateManager, never()).getActiveDatabase();
            verify(dialogService, never()).showCustomDialogAndWait(any(Dialog.class));
        }
    }

    // [utest->req~git.commit.initialize-repository~1]
    @Test
    void committingLibraryOutsideRepositoryInitializesRepositoryAndAddsOnlyTheLibrary() throws Exception {
        Path libraryFile = libraryDirectory.resolve("library.bib");
        Files.writeString(libraryFile, "@Article{test,}");
        Path unrelatedFile = libraryDirectory.resolve("notes.txt");
        Files.writeString(unrelatedFile, "not part of the library");

        executeCommitAction(libraryFile, true);

        assertEquals(Optional.of(libraryDirectory.toAbsolutePath()), GitHandler.findRepositoryRoot(libraryFile));
        try (Git git = Git.open(libraryDirectory.toFile())) {
            assertEquals(Set.of("notes.txt"), git.status().call().getUntracked());
        }
    }

    // [utest->req~git.commit.initialize-repository~1]
    @Test
    void cancellingLeavesTheDirectoryUntouched() throws Exception {
        Path libraryFile = libraryDirectory.resolve("library.bib");
        Files.writeString(libraryFile, "@Article{test,}");

        executeCommitAction(libraryFile, false);

        assertEquals(Optional.empty(), GitHandler.findRepositoryRoot(libraryFile));
        try (Stream<Path> files = Files.list(libraryDirectory)) {
            assertEquals(Set.of(libraryFile), files.collect(Collectors.toSet()));
        }
    }

    private void executeCommitAction(Path libraryFile, boolean confirmInitialization) {
        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        when(databaseContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);
        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(libraryFile));
        StateManager initStateManager = new JabRefGuiStateManager();
        initStateManager.activeDatabaseProperty().setValue(Optional.of(databaseContext));

        DialogService initDialogService = mock(DialogService.class);
        when(initDialogService.showConfirmationDialogAndWait(anyString(), anyString(), anyString(), anyString())).thenReturn(confirmInitialization);

        // tabSupplier yields null: without an open tab there is nothing to save first
        new GitCommitAction(
                () -> null,
                initDialogService,
                initStateManager,
                preferences,
                mock(BibEntryTypesManager.class),
                mock(JournalAbbreviationRepository.class),
                new CurrentThreadTaskExecutor(),
                new GitHandlerRegistry(mock(GitPreferences.class))).execute();
    }

    private void clickButton(String label) {
        when(dialogService.showCustomButtonDialogAndWait(any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> Stream.of(invocation.getArguments())
                                                .filter(ButtonType.class::isInstance)
                                                .map(ButtonType.class::cast)
                                                .filter(button -> label.equals(button.getText()))
                                                .findFirst());
    }
}
