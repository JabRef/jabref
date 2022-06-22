package org.jabref.gui.preferences.external;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.push.PushToApplication;
import org.jabref.gui.push.PushToApplicationSettings;
import org.jabref.gui.push.PushToApplicationsManager;
import org.jabref.gui.push.PushToEmacs;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.ExternalApplicationsPreferences;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.PushToApplicationPreferences;

import de.saxsys.mvvmfx.utils.validation.CompositeValidator;
import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;

public class ExternalTabViewModel implements PreferenceTabViewModel {

    private final StringProperty eMailReferenceSubjectProperty = new SimpleStringProperty("");
    private final BooleanProperty autoOpenAttachedFoldersProperty = new SimpleBooleanProperty();
    private final ListProperty<PushToApplication> pushToApplicationsListProperty = new SimpleListProperty<>();
    private final ObjectProperty<PushToApplication> selectedPushToApplicationProperty = new SimpleObjectProperty<>();
    private final StringProperty citeCommandProperty = new SimpleStringProperty("");
    private final BooleanProperty useCustomTerminalProperty = new SimpleBooleanProperty();
    private final StringProperty customTerminalCommandProperty = new SimpleStringProperty("");
    private final BooleanProperty useCustomFileBrowserProperty = new SimpleBooleanProperty();
    private final StringProperty customFileBrowserCommandProperty = new SimpleStringProperty("");

    private final Validator terminalCommandValidator;
    private final Validator fileBrowserCommandValidator;

    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final PushToApplicationsManager pushToApplicationsManager;

    private final FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder().build();

    private final ExternalApplicationsPreferences initialPreferences;
    private final ObjectProperty<PushToApplicationPreferences> workingPushToApplicationPreferences;

    public ExternalTabViewModel(DialogService dialogService, PreferencesService preferencesService, PushToApplicationsManager pushToApplicationsManager) {
        this.dialogService = dialogService;
        this.preferences = preferencesService;
        this.pushToApplicationsManager = pushToApplicationsManager;
        this.initialPreferences = preferences.getExternalApplicationsPreferences();
        this.workingPushToApplicationPreferences = new SimpleObjectProperty<>(preferencesService.getPushToApplicationPreferences());

        terminalCommandValidator = new FunctionBasedValidator<>(
                customTerminalCommandProperty,
                input -> !StringUtil.isNullOrEmpty(input),
                ValidationMessage.error(String.format("%s > %s %n %n %s",
                        Localization.lang("External programs"),
                        Localization.lang("Custom applications"),
                        Localization.lang("Please specify a terminal application."))));

        fileBrowserCommandValidator = new FunctionBasedValidator<>(
                customFileBrowserCommandProperty,
                input -> !StringUtil.isNullOrEmpty(input),
                ValidationMessage.error(String.format("%s > %s %n %n %s",
                        Localization.lang("External programs"),
                        Localization.lang("Custom applications"),
                        Localization.lang("Please specify a file browser."))));
    }

    public void setValues() {
        eMailReferenceSubjectProperty.setValue(initialPreferences.getEmailSubject());
        autoOpenAttachedFoldersProperty.setValue(initialPreferences.shouldAutoOpenEmailAttachmentsFolder());

        pushToApplicationsListProperty.setValue(
                FXCollections.observableArrayList(pushToApplicationsManager.getApplications()));
        selectedPushToApplicationProperty.setValue(
                pushToApplicationsManager.getApplicationByName(initialPreferences.getPushToApplicationName())
                                         .orElse(new PushToEmacs(dialogService, preferences)));

        citeCommandProperty.setValue(initialPreferences.getCiteCommand());
        useCustomTerminalProperty.setValue(initialPreferences.useCustomTerminal());
        customTerminalCommandProperty.setValue(initialPreferences.getCustomTerminalCommand());
        useCustomFileBrowserProperty.setValue(initialPreferences.useCustomFileBrowser());
        customFileBrowserCommandProperty.setValue(initialPreferences.getCustomFileBrowserCommand());
    }

    public void storeSettings() {
        preferences.storeExternalApplicationsPreferences(new ExternalApplicationsPreferences(
                eMailReferenceSubjectProperty.getValue(),
                autoOpenAttachedFoldersProperty.getValue(),
                selectedPushToApplicationProperty.getValue().getDisplayName(),
                citeCommandProperty.getValue(),
                useCustomTerminalProperty.getValue(),
                customTerminalCommandProperty.getValue(),
                useCustomFileBrowserProperty.getValue(),
                customFileBrowserCommandProperty.getValue()));

        preferences.storePushToApplicationPreferences(workingPushToApplicationPreferences.get());

        pushToApplicationsManager.updateApplicationAction(selectedPushToApplicationProperty.getValue());
    }

    public ValidationStatus terminalCommandValidationStatus() {
        return terminalCommandValidator.getValidationStatus();
    }

    public ValidationStatus fileBrowserCommandValidationStatus() {
        return fileBrowserCommandValidator.getValidationStatus();
    }

    public boolean validateSettings() {
        CompositeValidator validator = new CompositeValidator();

        if (useCustomTerminalProperty.getValue()) {
            validator.addValidators(terminalCommandValidator);
        }

        if (useCustomFileBrowserProperty.getValue()) {
            validator.addValidators(fileBrowserCommandValidator);
        }

        ValidationStatus validationStatus = validator.getValidationStatus();
        if (!validationStatus.isValid()) {
            validationStatus.getHighestMessage().ifPresent(message ->
                    dialogService.showErrorDialogAndWait(message.getMessage()));
            return false;
        }
        return true;
    }

    public void pushToApplicationSettings() {
        PushToApplication selectedApplication = selectedPushToApplicationProperty.getValue();
        PushToApplicationSettings settings = selectedApplication.getSettings(selectedApplication, workingPushToApplicationPreferences);

        DialogPane dialogPane = new DialogPane();
        dialogPane.setContent(settings.getSettingsPane());

        dialogService.showCustomDialogAndWait(
                Localization.lang("Application settings"),
                dialogPane,
                ButtonType.OK, ButtonType.CANCEL)
                     .ifPresent(btn -> {
                                 if (btn == ButtonType.OK) {
                                     settings.storeSettings();
                                 }
                             }
                     );
    }

    public void customTerminalBrowse() {
        dialogService.showFileOpenDialog(fileDialogConfiguration)
                     .ifPresent(file -> customTerminalCommandProperty.setValue(file.toAbsolutePath().toString()));
    }

    public void customFileBrowserBrowse() {
        dialogService.showFileOpenDialog(fileDialogConfiguration)
                     .ifPresent(file -> customFileBrowserCommandProperty.setValue(file.toAbsolutePath().toString()));
    }

    // EMail

    public StringProperty eMailReferenceSubjectProperty() {
        return this.eMailReferenceSubjectProperty;
    }

    public BooleanProperty autoOpenAttachedFoldersProperty() {
        return this.autoOpenAttachedFoldersProperty;
    }

    // Push-To-Application

    public ListProperty<PushToApplication> pushToApplicationsListProperty() {
        return this.pushToApplicationsListProperty;
    }

    public ObjectProperty<PushToApplication> selectedPushToApplication() {
        return this.selectedPushToApplicationProperty;
    }

    public StringProperty citeCommandProperty() {
        return this.citeCommandProperty;
    }

    // Open console

    public BooleanProperty useCustomTerminalProperty() {
        return this.useCustomTerminalProperty;
    }

    public StringProperty customTerminalCommandProperty() {
        return this.customTerminalCommandProperty;
    }

    // Open File Browser

    public BooleanProperty useCustomFileBrowserProperty() {
        return this.useCustomFileBrowserProperty;
    }

    public StringProperty customFileBrowserCommandProperty() {
        return this.customFileBrowserCommandProperty;
    }
}
