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
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.OptionalUtil;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.eclipse.jgit.api.errors.GitAPIException;

/// This dialog makes the connection to GitHub configurable
/// We do not go through the JabRef preferences dialog, because need the preferences close to the user
public class GitShareToGitHubDialogViewModel extends AbstractViewModel {
    private final StateManager stateManager;
    private final DialogService dialogService;

    // The preferences stored in JabRef
    private final GitPreferences gitPreferences;

    // The preferences of this dialog
    private final StringProperty username = new SimpleStringProperty("");
    private final StringProperty pat = new SimpleStringProperty("");

    // TODO: This should be a library preference -> the library is connected to repository; not all JabRef libraries to the same one
    //       Reason: One could have https://github.com/JabRef/JabRef-exmple-libraries as one repo and https://github.com/myexampleuser/demolibs as onther repository
    //               Both share the same secrets, but are different URLs.
    //       Also think of having two .bib files in the same folder - they will have the same repository URL -- should make no issues, but let's see...
    private final StringProperty repositoryUrl = new SimpleStringProperty("");
    private final BooleanProperty rememberPat = new SimpleBooleanProperty();

    private final Validator repositoryUrlValidator;
    private final Validator githubUsernameValidator;
    private final Validator githubPatValidator;

    public GitShareToGitHubDialogViewModel(GitPreferences gitPreferences, StateManager stateManager, DialogService dialogService) {
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.gitPreferences = gitPreferences;

        // Copy the existing preferences and make them available for modification
//        localGitPreferences = GitPreferences.of(preferences);

        repositoryUrlValidator = new FunctionBasedValidator<>(
                repositoryUrl,
                githubHttpsUrlValidator(),
                ValidationMessage.error(Localization.lang("Repository URL is required"))
        );
        githubUsernameValidator = new FunctionBasedValidator<>(
                username,
                notEmptyValidator(),
                ValidationMessage.error(Localization.lang("GitHub username is required"))
        );
        githubPatValidator = new FunctionBasedValidator<>(
                pat,
                notEmptyValidator(),
                ValidationMessage.error(Localization.lang("Personal Access Token is required"))
        );
    }

    public void shareToGitHub() throws JabRefException, IOException, GitAPIException {
        String url = OptionalUtil.fromStringProperty(repositoryUrl).orElse("");
        String user = OptionalUtil.fromStringProperty(username).orElse("");
        String token = OptionalUtil.fromStringProperty(pat).orElse("");

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

        handler.setCredentials(user, token);

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

        storeSettings();
    }

    public void setValues() {
        // TODO: Change this to be in line with proxy preferences
        //       - [ ] Rewrite from Optional to plain String, because lifecycle ensures that always "something" is in there
        //       - See "defaults.put(PROXY_HOSTNAME, "");" in org.jabref.logic.preferences.JabRefCliPreferences.JabRefCliPreferences
        //       - [ ] Write documentation to docs/code-howtos/preferences.md
        repositoryUrl.set(gitPreferences.getRepositoryUrl().orElse(""));
        username.set(gitPreferences.getUsername().orElse(""));
        pat.set(gitPreferences.getPat().orElse(""));
        rememberPat.set(gitPreferences.getRememberPat());
    }

    public void storeSettings() {
        gitPreferences.setRepositoryUrl(repositoryUrl.get().trim());
        gitPreferences.setUsername(username.get().trim());
        gitPreferences.setRememberPat(rememberPat.get());

        if (rememberPat.get()) {
            gitPreferences.setPat(pat.get().trim());
        }
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
        return input -> StringUtil.isNotBlank(input);
    }

    private Predicate<String> githubHttpsUrlValidator() {
        return input -> StringUtil.isNotBlank(input) && input.trim().matches("^https://.+");
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public StringProperty patProperty() {
        return pat;
    }

    public StringProperty repositoryUrlProperty() {
        return repositoryUrl;
    }

    public BooleanProperty rememberPatProperty() {
        return rememberPat;
    }
}
