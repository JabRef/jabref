package org.jabref.gui.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.logic.JabRefException;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.GitHubRepositoryAccess;
import org.jabref.logic.git.GitHubRepositoryAccessChecker;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.git.status.GitStatusSnapshot;
import org.jabref.logic.git.status.SyncStatus;
import org.jabref.logic.git.util.GitExceptionUtil;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.git.util.GitInitService;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.URLUtil;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Shares the active library to a GitHub repository.
/// The credentials are taken from the Git preferences.
public class GitShareToGitHubDialogViewModel extends AbstractViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitShareToGitHubDialogViewModel.class);

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final GitHandlerRegistry gitHandlerRegistry;
    private final GitHubRepositoryAccessChecker gitHubRepositoryAccessChecker;

    private final GitPreferences gitPreferences;

    private final StringProperty repositoryUrlProperty = new SimpleStringProperty("");

    private final Validator repositoryUrlValidator;

    public GitShareToGitHubDialogViewModel(
            GitPreferences gitPreferences,
            StateManager stateManager,
            DialogService dialogService,
            TaskExecutor taskExecutor,
            GitHandlerRegistry gitHandlerRegistry) {
        this(gitPreferences, stateManager, dialogService, taskExecutor, gitHandlerRegistry, new GitHubRepositoryAccessChecker());
    }

    GitShareToGitHubDialogViewModel(
            GitPreferences gitPreferences,
            StateManager stateManager,
            DialogService dialogService,
            TaskExecutor taskExecutor,
            GitHandlerRegistry gitHandlerRegistry,
            GitHubRepositoryAccessChecker gitHubRepositoryAccessChecker) {
        this.stateManager = stateManager;
        this.gitPreferences = gitPreferences;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.gitHandlerRegistry = gitHandlerRegistry;
        this.gitHubRepositoryAccessChecker = gitHubRepositoryAccessChecker;

        repositoryUrlValidator = new FunctionBasedValidator<>(
                repositoryUrlProperty,
                githubHttpsUrlValidator(),
                ValidationMessage.error(Localization.lang("Please enter a valid HTTPS GitHub repository URL"))
        );
    }

    /// @implNote `close` Is a runnable to make testing easier
    public void shareToGitHub(Runnable close) {
        // We store the settings because "Share" implies that the settings should be used as typed
        this.storeSettings();
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
                    LOGGER.warn("GitHub share failed", e);
                    String message;
                    if (e instanceof JabRefException jabRefException) {
                        message = jabRefException.getLocalizedMessage();
                    } else if (GitExceptionUtil.isLockFailure(e)) {
                        message = Localization.lang("The Git repository is locked. Close other Git, JabRef, or IDE processes and try again.");
                    } else {
                        message = Localization.lang("Could not share this library to GitHub. Please check the repository and try again.");
                    }
                    dialogService.showErrorDialogAndWait(
                            Localization.lang("GitHub share failed"),
                            message
                    );
                })
                .executeWith(taskExecutor);
    }

    /// Method assumes that settings are stored before.
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

        if (StringUtil.isBlank(gitPreferences.getUsername()) || StringUtil.isBlank(gitPreferences.getPat())) {
            throw new JabRefException(Localization.lang("No GitHub credentials. Please configure them in Preferences > Git."));
        }

        // We don't get a new preference object (and re-use the existing one instead), because of ADR-0016

        // TODO: Read remove from the git configuration - and only prompt for a repository if there is none
        String url = gitPreferences.getRepositoryUrl();

        Path bibPath = GitHandler.resolveToRealPath(bibFilePathOpt.get());
        GitInitService.initRepoAndSetRemote(bibPath, url, gitHandlerRegistry);
        GitHandler handler = gitHandlerRegistry.fromAnyPath(bibPath)
                                               .orElseThrow(() -> new JabRefException(
                                                       "Could not resolve the Git repository for the current library",
                                                       Localization.lang("Could not set up the Git repository for this library. Please check the repository location and try again.")));
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

    public void setValues() {
        repositoryUrlProperty.set(gitPreferences.getRepositoryUrl());
    }

    public void storeSettings() {
        gitPreferences.setRepositoryUrl(repositoryUrlProperty.get().trim());
    }

    public void checkGitHubAccess() {
        // [impl->req~git.share.personal-access-token-verification~1]
        BackgroundTask
                .wrap(() -> gitHubRepositoryAccessChecker.check(repositoryUrlProperty.get().trim(), gitPreferences.getUsername(), gitPreferences.getPat()))
                .onSuccess(this::showGitHubAccessResult)
                .onFailure(e -> {
                    LOGGER.debug("Could not check GitHub repository access", e);
                    dialogService.showErrorDialogAndWait(
                            Localization.lang("GitHub access"),
                            Localization.lang("Could not connect to GitHub. Please check your network connection and try again."));
                })
                .executeWith(taskExecutor);
    }

    private void showGitHubAccessResult(GitHubRepositoryAccess access) {
        String title = Localization.lang("GitHub access");
        switch (access) {
            case WRITE_ACCESS ->
                    dialogService.showInformationDialogAndWait(
                            title,
                            Localization.lang("Personal access token has push access to this repository."));
            case INVALID_TOKEN ->
                    dialogService.showErrorDialogAndWait(
                            title,
                            Localization.lang("Personal access token is invalid."));
            case REPOSITORY_NOT_ACCESSIBLE ->
                    dialogService.showErrorDialogAndWait(
                            title,
                            Localization.lang("The personal access token cannot push to this repository."));
            case INVALID_REPOSITORY_URL ->
                    dialogService.showErrorDialogAndWait(
                            title,
                            Localization.lang("Please enter a valid GitHub repository URL."));
        }
    }

    public ValidationStatus repositoryUrlValidation() {
        return repositoryUrlValidator.getValidationStatus();
    }

    private Predicate<String> githubHttpsUrlValidator() {
        return input -> StringUtil.isNotBlank(input) && URLUtil.isURL(input.trim());
    }

    public StringProperty repositoryUrlProperty() {
        return repositoryUrlProperty;
    }
}
