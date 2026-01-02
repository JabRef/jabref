package org.jabref.gui.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

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
import org.jabref.logic.util.URLUtil;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.eclipse.jgit.api.errors.GitAPIException;

/// "Preferences" dialog for sharing library to GitHub.
/// We do not put it into the JabRef preferences dialog because we want these settings to be close to the user.
public class GitShareToGitHubDialogViewModel extends AbstractViewModel {
    private final StateManager stateManager;

    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final GitHandlerRegistry gitHandlerRegistry;

    // The preferences stored in JabRef
    private final GitPreferences gitPreferences;

    // The preferences of this dialog
    private final StringProperty usernameProperty = new SimpleStringProperty("");
    private final StringProperty patProperty = new SimpleStringProperty("");

    // TODO: This should be a library preference -> the library is connected to repository; not all JabRef libraries to the same one
    //       Reason: One could have https://github.com/JabRef/JabRef-exmple-libraries as one repo and https://github.com/myexampleuser/demolibs as another repository
    //               Both share the same secrets, but are different URLs.
    //       Also think of having two .bib files in the same folder - they will have the same repository URL -- should make no issues, but let's see...
    private final StringProperty repositoryUrlProperty = new SimpleStringProperty("");

    private final BooleanProperty rememberPatProperty = new SimpleBooleanProperty();

    private final Validator repositoryUrlValidator;
    private final Validator githubUsernameValidator;
    private final Validator githubPatValidator;

    public GitShareToGitHubDialogViewModel(
            GitPreferences gitPreferences,
            StateManager stateManager,
            DialogService dialogService,
            TaskExecutor taskExecutor,
            GitHandlerRegistry gitHandlerRegistry) {
        this.stateManager = stateManager;
        this.gitPreferences = gitPreferences;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.gitHandlerRegistry = gitHandlerRegistry;

        repositoryUrlValidator = new FunctionBasedValidator<>(
                repositoryUrlProperty,
                githubHttpsUrlValidator(),
                ValidationMessage.error(Localization.lang("Please enter a valid HTTPS GitHub repository URL"))
        );
        githubUsernameValidator = new FunctionBasedValidator<>(
                usernameProperty,
                notEmptyValidator(),
                ValidationMessage.error(Localization.lang("GitHub username is required"))
        );
        githubPatValidator = new FunctionBasedValidator<>(
                patProperty,
                notEmptyValidator(),
                ValidationMessage.error(Localization.lang("Personal Access Token is required"))
        );
    }

    /// @implNote `close` Is a runnable to make testing easier
    public void shareToGitHub(Runnable close) {
        // We store the settings because "Share" implies that the settings should be used as typed
        // We also have the option to not store the settings permanently: This is implemented in JabRefCliPreferences at the listeners.
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
                .onFailure(e ->
                        dialogService.showErrorDialogAndWait(
                                Localization.lang("GitHub share failed"),
                                e.getMessage(),
                                e
                        )
                )
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

    public void setValues() {
        repositoryUrlProperty.set(gitPreferences.getRepositoryUrl());
        usernameProperty.set(gitPreferences.getUsername());
        patProperty.set(gitPreferences.getPat());
        rememberPatProperty.set(gitPreferences.getPersistPat());
    }

    public void storeSettings() {
        gitPreferences.setRepositoryUrl(repositoryUrlProperty.get().trim());
        gitPreferences.setUsername(usernameProperty.get().trim());
        gitPreferences.setPersistPat(rememberPatProperty.get());
        gitPreferences.setPat(patProperty.get().trim());
    }

    public ValidationStatus repositoryUrlValidation() {
        return repositoryUrlValidator.getValidationStatus();
    }

    public ValidationStatus githubUsernameValidation() {
        return githubUsernameValidator.getValidationStatus();
    }

    public ValidationStatus githubPatValidation() {
        return githubPatValidator.getValidationStatus();
    }

    private Predicate<String> notEmptyValidator() {
        return StringUtil::isNotBlank;
    }

    private Predicate<String> githubHttpsUrlValidator() {
        return input -> StringUtil.isNotBlank(input) && URLUtil.isURL(input.trim());
    }

    public StringProperty usernameProperty() {
        return usernameProperty;
    }

    public StringProperty patProperty() {
        return patProperty;
    }

    public StringProperty repositoryUrlProperty() {
        return repositoryUrlProperty;
    }

    public BooleanProperty rememberPatProperty() {
        return rememberPatProperty;
    }
}
