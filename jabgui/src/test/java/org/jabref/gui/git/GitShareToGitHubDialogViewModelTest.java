package org.jabref.gui.git;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.logic.git.GitHubRepositoryAccess;
import org.jabref.logic.git.GitHubRepositoryAccessChecker;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GitShareToGitHubDialogViewModelTest {

    private static final String REPOSITORY_URL = "https://github.com/JabRef/jabref.git";

    private DialogService dialogService;
    private StateManager stateManager;
    private GitHubRepositoryAccessChecker gitHubRepositoryAccessChecker;

    @BeforeEach
    void setUp() {
        dialogService = mock(DialogService.class);
        stateManager = mock(StateManager.class);
        gitHubRepositoryAccessChecker = mock(GitHubRepositoryAccessChecker.class);
    }

    private GitShareToGitHubDialogViewModel viewModel(GitPreferences gitPreferences) {
        return new GitShareToGitHubDialogViewModel(
                gitPreferences,
                stateManager,
                dialogService,
                new CurrentThreadTaskExecutor(),
                mock(GitHandlerRegistry.class),
                gitHubRepositoryAccessChecker);
    }

    private GitPreferences gitPreferences(String username, String pat) {
        return new GitPreferences(username, pat, "", false, 5);
    }

    @Test
    void checkGitHubAccessReportsPushAccess() {
        GitShareToGitHubDialogViewModel viewModel = viewModel(gitPreferences("JabRef", "token"));
        viewModel.repositoryUrlProperty().set(REPOSITORY_URL);
        when(gitHubRepositoryAccessChecker.check(anyString(), anyString(), anyString())).thenReturn(GitHubRepositoryAccess.WRITE_ACCESS);

        viewModel.checkGitHubAccess();

        verify(dialogService).showInformationDialogAndWait(
                Localization.lang("GitHub access"),
                Localization.lang("Personal access token has push access to this repository."));
    }

    @Test
    void checkGitHubAccessReportsMissingPushAccess() {
        GitShareToGitHubDialogViewModel viewModel = viewModel(gitPreferences("JabRef", "token"));
        viewModel.repositoryUrlProperty().set(REPOSITORY_URL);
        when(gitHubRepositoryAccessChecker.check(anyString(), anyString(), anyString())).thenReturn(GitHubRepositoryAccess.REPOSITORY_NOT_ACCESSIBLE);

        viewModel.checkGitHubAccess();

        verify(dialogService).showErrorDialogAndWait(
                Localization.lang("GitHub access"),
                Localization.lang("The personal access token cannot push to this repository."));
    }

    @Test
    void checkGitHubAccessUsesTrimmedUrlAndConfiguredCredentials() {
        GitShareToGitHubDialogViewModel viewModel = viewModel(gitPreferences("JabRef", "token"));
        viewModel.repositoryUrlProperty().set(" " + REPOSITORY_URL + " ");
        when(gitHubRepositoryAccessChecker.check(anyString(), anyString(), anyString())).thenReturn(GitHubRepositoryAccess.WRITE_ACCESS);

        viewModel.checkGitHubAccess();

        verify(gitHubRepositoryAccessChecker).check(REPOSITORY_URL, "JabRef", "token");
    }

    @Test
    void shareToGitHubFailsWithoutConfiguredCredentials() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        databaseContext.setDatabasePath(Path.of("library.bib"));
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));

        GitShareToGitHubDialogViewModel viewModel = viewModel(gitPreferences("", ""));
        viewModel.repositoryUrlProperty().set(REPOSITORY_URL);

        viewModel.shareToGitHub(() -> {
        });

        verify(dialogService).showErrorDialogAndWait(
                Localization.lang("GitHub share failed"),
                Localization.lang("No GitHub credentials. Please configure them in Preferences > Git."));
    }

    @Test
    void shareToGitHubShowsErrorWithoutExceptionDetails() {
        when(stateManager.getActiveDatabase()).thenReturn(Optional.empty());

        GitShareToGitHubDialogViewModel viewModel = viewModel(gitPreferences("JabRef", "token"));
        viewModel.shareToGitHub(() -> {
        });

        verify(dialogService).showErrorDialogAndWait(
                Localization.lang("GitHub share failed"),
                Localization.lang("No library open"));
    }
}
