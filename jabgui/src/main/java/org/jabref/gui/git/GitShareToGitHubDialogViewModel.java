package org.jabref.gui.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.logic.JabRefException;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.git.status.GitStatusSnapshot;
import org.jabref.logic.git.status.SyncStatus;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.git.util.GitInitService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Shares the active library to the GitHub repository configured in the preferences.
public class GitShareToGitHubDialogViewModel extends AbstractViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitShareToGitHubDialogViewModel.class);

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final GitHandlerRegistry gitHandlerRegistry;
    private final GitPreferences gitPreferences;

    public GitShareToGitHubDialogViewModel(
            GitPreferences gitPreferences,
            StateManager stateManager,
            DialogService dialogService,
            TaskExecutor taskExecutor,
            GitHandlerRegistry gitHandlerRegistry) {
        this.gitPreferences = gitPreferences;
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.gitHandlerRegistry = gitHandlerRegistry;
    }

    public String getRepositoryUrl() {
        return gitPreferences.getRepositoryUrl();
    }

    /// @implNote `close` Is a runnable to make testing easier
    public void shareToGitHub(Runnable close) {
        BackgroundTask
                .wrap(() -> {
                    this.doShareToGitHub();
                    return null;
                })
                .onSuccess(_ -> {
                    dialogService.notify(Localization.lang("Successfully pushed to GitHub."));
                    close.run();
                })
                .onFailure(e -> {
                    LOGGER.warn("Git share failed", e);
                    dialogService.showErrorDialogAndWait(
                            Localization.lang("GitHub share failed"),
                            e.getMessage(),
                            e
                    );
                })
                .executeWith(taskExecutor);
    }

    private void doShareToGitHub() throws JabRefException, IOException, GitAPIException {
        Optional<BibDatabaseContext> activeDatabaseOpt = stateManager.getActiveDatabase();
        if (activeDatabaseOpt.isEmpty()) {
            throw new JabRefException(Localization.lang("No library open"));
        }

        BibDatabaseContext activeDatabase = activeDatabaseOpt.get();
        Optional<Path> bibFilePathOpt = activeDatabase.getDatabasePath();
        if (bibFilePathOpt.isEmpty()) {
            throw new JabRefException(Localization.lang("No library file path. Please save the library to a file first."));
        }

        // We don't get a new preference object (and re-use the existing one instead), because of ADR-0016

        // TODO: Read remove from the git configuration - and only prompt for a repository if there is none
        String url = gitPreferences.getRepositoryUrl();

        Path bibPath = bibFilePathOpt.get();
        GitInitService.initRepoAndSetRemote(bibPath, url, gitHandlerRegistry);
        GitHandler handler = gitHandlerRegistry.get(bibPath.getParent());
        GitStatusSnapshot status = GitStatusChecker.checkStatusAndFetch(handler);
        if (status.syncStatus() == SyncStatus.BEHIND) {
            throw new JabRefException(Localization.lang("Remote repository is not empty. Please pull changes before pushing."));
        }
        handler.createCommitOnCurrentBranch(Localization.lang("Share library to GitHub"), false);
        if (status.syncStatus() == SyncStatus.REMOTE_EMPTY) {
            handler.pushCurrentBranchCreatingUpstream();
        } else {
            handler.pushCommitsToRemoteRepository();
        }
    }
}
