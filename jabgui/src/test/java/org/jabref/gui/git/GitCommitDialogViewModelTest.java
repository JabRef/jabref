package org.jabref.gui.git;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.StateManager;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.git.status.GitStatusSnapshot;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import org.jspecify.annotations.NullMarked;

import org.eclipse.jgit.api.errors.JGitInternalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class GitCommitDialogViewModelTest {

    private StateManager stateManager;
    private DialogService dialogService;
    private GitHandlerRegistry gitHandlerRegistry;
    private GitHandler gitHandler;
    private BibDatabaseContext databaseContext;
    private GitCommitDialogViewModel viewModel;

    @BeforeEach
    void setUp() {
        stateManager = new JabRefGuiStateManager();
        dialogService = mock(DialogService.class);
        gitHandlerRegistry = mock(GitHandlerRegistry.class);
        gitHandler = mock(GitHandler.class);
        databaseContext = mock(BibDatabaseContext.class);

        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(Path.of("library.bib")));
        stateManager.activeDatabaseProperty().setValue(Optional.of(databaseContext));

        viewModel = new GitCommitDialogViewModel(
                stateManager,
                dialogService,
                new CurrentThreadTaskExecutor(),
                gitHandlerRegistry);
    }

    @Test
    void commitShowsGracefulErrorWhenGitCommitFails() throws Exception {
        Path libraryPath = Path.of("library.bib");
        Path repositoryPath = Path.of("repository");

        when(gitHandlerRegistry.get(repositoryPath)).thenReturn(gitHandler);

        try (MockedStatic<GitHandler> gitHandlerStatic = Mockito.mockStatic(GitHandler.class);
             MockedStatic<GitStatusChecker> statusCheckerStatic = Mockito.mockStatic(GitStatusChecker.class)) {

            gitHandlerStatic.when(() -> GitHandler.findRepositoryRoot(libraryPath))
                            .thenReturn(Optional.of(repositoryPath));

            GitStatusSnapshot status = mock(GitStatusSnapshot.class);
            when(status.tracking()).thenReturn(true);
            when(status.conflict()).thenReturn(false);
            statusCheckerStatic.when(() -> GitStatusChecker.checkStatus(gitHandler))
                               .thenReturn(status);

            when(gitHandler.createCommitOnCurrentBranch(any(), any(boolean.class)))
                    .thenThrow(new JGitInternalException("Cannot lock Git index"));

            viewModel.commit(() -> {});

            verify(dialogService).showErrorDialogAndWait(
                    Localization.lang("Git Commit Failed"),
                    Localization.lang("Could not create the Git commit. Please check the repository and try again."));
        }
    }
}