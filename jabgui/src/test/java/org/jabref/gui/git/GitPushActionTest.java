package org.jabref.gui.git;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.GitSyncService;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.errors.JGitInternalException;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class GitPushActionTest {

    private ImportFormatPreferences importFormatPreferences;
    private DialogService dialogService;
    private StateManager stateManager;
    private GuiPreferences guiPreferences;
    private GitHandlerRegistry gitHandlerRegistry;
    private TaskExecutor taskExecutor;
    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() {
        dialogService = mock(DialogService.class);
        stateManager = new JabRefGuiStateManager();
        guiPreferences = mock(GuiPreferences.class);
        importFormatPreferences = mock(ImportFormatPreferences.class);
        when(guiPreferences.getImportFormatPreferences()).thenReturn(importFormatPreferences);
        gitHandlerRegistry = mock(GitHandlerRegistry.class);
        taskExecutor = new CurrentThreadTaskExecutor();
        databaseContext = mock(BibDatabaseContext.class);

        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(Path.of("library.bib")));
        stateManager.activeDatabaseProperty().setValue(Optional.of(databaseContext));
    }

    @Test
    void pushShowsGracefulErrorWhenGitPushFails() throws Exception {
        Path libraryPath = Path.of("library.bib");

        GitHandler gitHandler = mock(GitHandler.class);
        GitStatusViewModel gitStatusViewModel = mock(GitStatusViewModel.class);

        when(gitHandlerRegistry.fromAnyPath(libraryPath))
                .thenReturn(Optional.of(gitHandler));

        GitPushAction action = new GitPushAction(
                dialogService,
                stateManager,
                guiPreferences,
                taskExecutor,
                gitHandlerRegistry);

        try (MockedStatic<GitSyncService> gitSyncServiceStatic =
                     Mockito.mockStatic(GitSyncService.class);
             MockedStatic<GitStatusViewModel> gitStatusViewModelStatic =
                     Mockito.mockStatic(GitStatusViewModel.class)) {
            gitStatusViewModelStatic.when(() -> GitStatusViewModel.fromPathAndContext(
                    stateManager,
                    taskExecutor,
                    gitHandlerRegistry,
                    libraryPath))
                    .thenReturn(gitStatusViewModel);

            GitSyncService syncService = mock(GitSyncService.class);

            gitSyncServiceStatic.when(() -> GitSyncService.create(
                    importFormatPreferences,
                    gitHandlerRegistry))
                    .thenReturn(syncService);

            JGitInternalException exception =
                    new JGitInternalException("Cannot lock Git index");

            when(syncService.push(databaseContext, libraryPath))
                    .thenThrow(exception);

            action.execute();

            verify(dialogService).showErrorDialogAndWait(
                    Localization.lang("Git Push Failed"),
                    Localization.lang("Unexpected error: %0", exception.getMessage()));
        }
    }
}
