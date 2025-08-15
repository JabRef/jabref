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
import org.jabref.logic.git.prefs.GitPreferences;
import org.jabref.logic.git.status.GitStatusChecker;
import org.jabref.logic.git.status.GitStatusSnapshot;
import org.jabref.logic.git.status.SyncStatus;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.git.util.GitInitService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitShareToGitHubDialogViewModel extends AbstractViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitShareToGitHubDialogViewModel.class);

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final GitPreferences gitPreferences = new GitPreferences();

    private final StringProperty githubUsername = new SimpleStringProperty();
    private final StringProperty githubPat = new SimpleStringProperty();
    private final StringProperty repositoryUrl = new SimpleStringProperty();
    private final BooleanProperty rememberSettings = new SimpleBooleanProperty();
    private final Validator repositoryUrlValidator;
    private final Validator githubUsernameValidator;
    private final Validator githubPatValidator;

    public GitShareToGitHubDialogViewModel(StateManager stateManager, DialogService dialogService) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;

        repositoryUrlValidator = new FunctionBasedValidator<>(repositoryUrl, githubHttpsUrlValidator(), ValidationMessage.error(Localization.lang("Repository URL is required")));
        githubUsernameValidator = new FunctionBasedValidator<>(githubUsername, notEmptyValidator(), ValidationMessage.error(Localization.lang("GitHub username is required")));
        githubPatValidator = new FunctionBasedValidator<>(githubPat, notEmptyValidator(), ValidationMessage.error(Localization.lang("Personal Access Token is required")));

        applyGitPreferences();
    }

    public void shareToGitHub() throws JabRefException, IOException, GitAPIException {
        String url = trimOrEmpty(repositoryUrl.get());
        String user = trimOrEmpty(githubUsername.get());
        String pat = trimOrEmpty(githubPat.get());

        Optional<BibDatabaseContext> activeDatabaseOpt = stateManager.getActiveDatabase();
        if (activeDatabaseOpt.isEmpty()) {
            throw new JabRefException(Localization.lang("No library open"));
        }

        BibDatabaseContext activeDatabase = activeDatabaseOpt.get();
        Optional<Path> bibFilePathOpt = activeDatabase.getDatabasePath();
        if (bibFilePathOpt.isEmpty()) {
            throw new JabRefException(Localization.lang("No library file path. Please save the library to a file first."));
        }

        Path bibPath = bibFilePathOpt.get();

        GitInitService.initRepoAndSetRemote(bibPath, url);

        GitHandlerRegistry registry = new GitHandlerRegistry();
        GitHandler handler = registry.get(bibPath.getParent());

        handler.setCredentials(user, pat);

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

        setGitPreferences(url, user, pat);
    }

    private void applyGitPreferences() {
        gitPreferences.getUsername().ifPresent(githubUsername::set);
        gitPreferences.getPersonalAccessToken().ifPresent(token -> {
            githubPat.set(token);
            rememberSettings.set(true);
        });
        gitPreferences.getRepositoryUrl().ifPresent(repositoryUrl::set);
        rememberSettings.set(gitPreferences.getRememberPat() || rememberSettings.get());
    }

    private void setGitPreferences(String url, String user, String pat) {
        gitPreferences.setUsername(user);
        gitPreferences.setRepositoryUrl(url);
        gitPreferences.setRememberPat(rememberSettings.get());

        if (rememberSettings.get()) {
            gitPreferences.setPersonalAccessToken(pat);
        } else {
            gitPreferences.clearGitHubPersonalAccessToken();
        }
    }

    private static String trimOrEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    public StringProperty githubUsernameProperty() {
        return githubUsername;
    }

    public StringProperty githubPatProperty() {
        return githubPat;
    }

    public BooleanProperty rememberSettingsProperty() {
        return rememberSettings;
    }

    public StringProperty repositoryUrlProperty() {
        return repositoryUrl;
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
        return input -> input != null && !input.trim().isEmpty();
    }

    private Predicate<String> githubHttpsUrlValidator() {
        return input -> {
            if (input == null || input.trim().isEmpty()) {
                return false;
            }
            return input.trim().matches("^https://.+");
        };
    }
}
