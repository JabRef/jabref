package org.jabref.gui.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

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

    public GitShareToGitHubDialogViewModel(StateManager stateManager, DialogService dialogService) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;

        applyGitPreferences();
    }

    public boolean shareToGitHub() {
        String url = trimOrEmpty(repositoryUrl.get());
        String user = trimOrEmpty(githubUsername.get());
        String pat = trimOrEmpty(githubPat.get());

        if (url.isBlank()) {
            dialogService.showErrorDialogAndWait(Localization.lang("GitHub repository URL is required"));
            return false;
        }

        if (pat.isBlank()) {
            dialogService.showErrorDialogAndWait(Localization.lang("Personal Access Token is required to push"));
            return false;
        }
        if (user.isBlank()) {
            dialogService.showErrorDialogAndWait(Localization.lang("GitHub username is required"));
            return false;
        }
        Optional<BibDatabaseContext> activeDatabaseOpt = stateManager.getActiveDatabase();
        if (activeDatabaseOpt.isEmpty()) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("No library open")
            );
            return false;
        }

        BibDatabaseContext activeDatabase = activeDatabaseOpt.get();
        Optional<Path> bibFilePathOpt = activeDatabase.getDatabasePath();
        if (bibFilePathOpt.isEmpty()) {
            dialogService.showErrorDialogAndWait(
                    Localization.lang("No library file path"),
                    Localization.lang("Cannot share: Please save the library to a file first.")
            );
            return false;
        }

        Path bibPath = bibFilePathOpt.get();

        try {
            GitInitService.initRepoAndSetRemote(bibPath, url);
        } catch (JabRefException e) {
            dialogService.showErrorDialogAndWait(Localization.lang("Git error"), e.getMessage(), e);
            return false;
        }

        GitHandlerRegistry registry = new GitHandlerRegistry();
        GitHandler handler = registry.get(bibPath.getParent());

        boolean hasStoredPat = gitPreferences.getPersonalAccessToken().isPresent();
        if (!rememberSettingsProperty().get() || !hasStoredPat) {
            handler.setCredentials(user, pat);
        }

        GitStatusSnapshot status;
        try {
            status = GitStatusChecker.checkStatusAndFetch(handler);
        } catch (IOException | JabRefException e) {
            dialogService.showErrorDialogAndWait(Localization.lang("Cannot reach remote"), e);
            return false;
        }

        if (status.syncStatus() == SyncStatus.BEHIND) {
            dialogService.showWarningDialogAndWait(
                    Localization.lang("Remote repository is not empty"),
                    Localization.lang("Please pull changes before pushing.")
            );
            return false;
        }

        try {
            handler.createCommitOnCurrentBranch(Localization.lang("Share library to GitHub"), false);
        } catch (IOException | GitAPIException e) {
            dialogService.showErrorDialogAndWait(Localization.lang("Create commit failed", e));
        }

        try {
            if (status.syncStatus() == SyncStatus.REMOTE_EMPTY) {
                handler.pushCurrentBranchCreatingUpstream();
            } else {
                handler.pushCommitsToRemoteRepository();
            }
        } catch (IOException | GitAPIException e) {
            LOGGER.error("Push failed", e);
            dialogService.showErrorDialogAndWait(Localization.lang("Git error"), e);
            return false;
        } catch (JabRefException e) {
            dialogService.showErrorDialogAndWait(Localization.lang("Missing Git credentials"), e);
        }

        setGitPreferences(url, user, pat);

        dialogService.showInformationDialogAndWait(
                Localization.lang("GitHub Share"),
                Localization.lang("Successfully pushed to %0", url)
        );
        return true;
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
            boolean ok = gitPreferences.savePersonalAccessToken(pat, user);
            if (ok) {
                gitPreferences.setRememberPat(true);
            } else {
                gitPreferences.setRememberPat(false);
                dialogService.showErrorDialogAndWait(
                        Localization.lang("GitHub preferences not saved"),
                        Localization.lang("Failed to save Personal Access Token.")
                );
            }
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
}
