package org.jabref.gui.git;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.logic.git.GitHubRepositoryAccess;
import org.jabref.logic.git.GitHubRepositoryAccessChecker;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.CurrentThreadTaskExecutor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GitShareToGitHubDialogViewModelTest {

    private DialogService dialogService;
    private GitHubRepositoryAccessChecker gitHubRepositoryAccessChecker;
    private GitShareToGitHubDialogViewModel viewModel;

    @BeforeEach
    void setUp() {
        dialogService = mock(DialogService.class);
        gitHubRepositoryAccessChecker = mock(GitHubRepositoryAccessChecker.class);
        viewModel = new GitShareToGitHubDialogViewModel(
                GitPreferences.getDefault(),
                mock(StateManager.class),
                dialogService,
                new CurrentThreadTaskExecutor(),
                mock(GitHandlerRegistry.class),
                gitHubRepositoryAccessChecker);
    }

    @Test
    void checkGitHubAccessReportsPushAccess() {
        viewModel.repositoryUrlProperty().set("https://github.com/JabRef/jabref.git");
        viewModel.patProperty().set("token");
        when(gitHubRepositoryAccessChecker.check(anyString(), anyString(), anyString())).thenReturn(GitHubRepositoryAccess.WRITE_ACCESS);

        viewModel.checkGitHubAccess();

        verify(dialogService).showInformationDialogAndWait(
                Localization.lang("GitHub access"),
                Localization.lang("Personal access token has push access to this repository."));
    }

    @Test
    void checkGitHubAccessReportsMissingPushAccess() {
        viewModel.repositoryUrlProperty().set("https://github.com/JabRef/jabref.git");
        viewModel.patProperty().set("token");
        when(gitHubRepositoryAccessChecker.check(anyString(), anyString(), anyString())).thenReturn(GitHubRepositoryAccess.REPOSITORY_NOT_ACCESSIBLE);

        viewModel.checkGitHubAccess();

        verify(dialogService).showErrorDialogAndWait(
                Localization.lang("GitHub access"),
                Localization.lang("The personal access token cannot push to this repository."));
    }

    @Test
    void checkGitHubAccessTrimsValuesBeforeCheckingAccess() {
        viewModel.repositoryUrlProperty().set(" https://github.com/JabRef/jabref.git ");
        viewModel.usernameProperty().set(" JabRef ");
        viewModel.patProperty().set(" token ");
        when(gitHubRepositoryAccessChecker.check(anyString(), anyString(), anyString())).thenReturn(GitHubRepositoryAccess.WRITE_ACCESS);

        viewModel.checkGitHubAccess();

        verify(gitHubRepositoryAccessChecker).check("https://github.com/JabRef/jabref.git", "JabRef", "token");
    }
}
