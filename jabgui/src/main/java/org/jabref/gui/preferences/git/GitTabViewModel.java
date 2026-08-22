package org.jabref.gui.preferences.git;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.git.GitHubRepositoryAccess;
import org.jabref.logic.git.GitHubRepositoryAccessChecker;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitTabViewModel implements PreferenceTabViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitTabViewModel.class);

    private final StringProperty repositoryUrlProperty = new SimpleStringProperty("");
    private final StringProperty usernameProperty = new SimpleStringProperty("");
    private final StringProperty patProperty = new SimpleStringProperty("");
    private final BooleanProperty persistPatProperty = new SimpleBooleanProperty();
    private final BooleanProperty passwordPersistAvailable = new SimpleBooleanProperty();
    private final StringProperty pullIntervalProperty = new SimpleStringProperty("");

    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final GitPreferences gitPreferences;
    private final GitHubRepositoryAccessChecker accessChecker;

    private final Validator pullIntervalValidator;

    public GitTabViewModel(DialogService dialogService, TaskExecutor taskExecutor, GitPreferences gitPreferences) {
        this(dialogService, taskExecutor, gitPreferences, new GitHubRepositoryAccessChecker());
    }

    GitTabViewModel(DialogService dialogService,
                    TaskExecutor taskExecutor,
                    GitPreferences gitPreferences,
                    GitHubRepositoryAccessChecker accessChecker) {
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.gitPreferences = gitPreferences;
        this.accessChecker = accessChecker;

        pullIntervalValidator = new FunctionBasedValidator<>(
                pullIntervalProperty,
                input -> getIntervalAsInt(input).filter(interval -> interval > 0).isPresent(),
                ValidationMessage.error("%s > %s %n %n %s".formatted(
                        Localization.lang("Git"),
                        Localization.lang("Automatic operations"),
                        Localization.lang("You must enter a positive integer value"))));
    }

    @Override
    public void setValues() {
        repositoryUrlProperty.setValue(gitPreferences.getRepositoryUrl());
        usernameProperty.setValue(gitPreferences.getUsername());
        patProperty.setValue(gitPreferences.getPat());
        persistPatProperty.setValue(gitPreferences.getPersistPat());
        passwordPersistAvailable.setValue(OS.isKeyringAvailable());
        pullIntervalProperty.setValue(String.valueOf(gitPreferences.getPullInterval()));
    }

    @Override
    public void storeSettings() {
        gitPreferences.setRepositoryUrl(repositoryUrlProperty.getValue().trim());
        gitPreferences.setUsername(usernameProperty.getValue().trim());
        gitPreferences.setPersistPat(persistPatProperty.getValue());
        gitPreferences.setPat(patProperty.getValue().trim());
        gitPreferences.setPullInterval(Integer.parseInt(pullIntervalProperty.getValue().trim()));
    }

    @Override
    public boolean validateSettings() {
        ValidationStatus status = pullIntervalValidator.getValidationStatus();
        if (!status.isValid()) {
            status.getHighestMessage().ifPresent(message -> dialogService.showErrorDialogAndWait(message.getMessage()));
            return false;
        }
        return true;
    }

    public void checkGitHubAccess() {
        // [impl->req~ux.git-share.personal-access-token-verification~1]
        BackgroundTask
                .wrap(() -> accessChecker.check(repositoryUrlProperty.get().trim(), usernameProperty.get().trim(), patProperty.get().trim()))
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

    private Optional<Integer> getIntervalAsInt(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public StringProperty repositoryUrlProperty() {
        return repositoryUrlProperty;
    }

    public StringProperty usernameProperty() {
        return usernameProperty;
    }

    public StringProperty patProperty() {
        return patProperty;
    }

    public BooleanProperty persistPatProperty() {
        return persistPatProperty;
    }

    public ReadOnlyBooleanProperty passwordPersistAvailable() {
        return passwordPersistAvailable;
    }

    public StringProperty pullIntervalProperty() {
        return pullIntervalProperty;
    }

    public ValidationStatus pullIntervalValidationStatus() {
        return pullIntervalValidator.getValidationStatus();
    }
}
