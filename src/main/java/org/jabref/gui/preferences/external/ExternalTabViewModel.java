package org.jabref.gui.preferences.external;

import java.util.HashMap;

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
import org.jabref.gui.push.PushToApplications;
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

    private final FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder().build();

    private final ExternalApplicationsPreferences initialExternalApplicationPreferences;
    private final PushToApplicationPreferences initialPushToApplicationPreferences;
    private final PushToApplicationPreferences workingPushToApplicationPreferences;

    public ExternalTabViewModel(DialogService dialogService, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.preferences = preferencesService;
        this.initialExternalApplicationPreferences = preferences.getExternalApplicationsPreferences();
        this.initialPushToApplicationPreferences = preferences.getPushToApplicationPreferences();
        this.workingPushToApplicationPreferences = new PushToApplicationPreferences(
                initialPushToApplicationPreferences.getActiveApplicationName(),
                new HashMap<>(initialPushToApplicationPreferences.getCommandPaths()),
                initialPushToApplicationPreferences.getEmacsArguments(),
                initialPushToApplicationPreferences.getVimServer());

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
        eMailReferenceSubjectProperty.setValue(initialExternalApplicationPreferences.getEmailSubject());
        autoOpenAttachedFoldersProperty.setValue(initialExternalApplicationPreferences.shouldAutoOpenEmailAttachmentsFolder());

        pushToApplicationsListProperty.setValue(
                FXCollections.observableArrayList(PushToApplications.getAllApplications(dialogService, preferences)));
        selectedPushToApplicationProperty.setValue(
                PushToApplications.getApplicationByName(initialPushToApplicationPreferences.getActiveApplicationName(), dialogService, preferences)
                                  .orElse(new PushToEmacs(dialogService, preferences)));

        citeCommandProperty.setValue(initialExternalApplicationPreferences.getCiteCommand());
        useCustomTerminalProperty.setValue(initialExternalApplicationPreferences.useCustomTerminal());
        customTerminalCommandProperty.setValue(initialExternalApplicationPreferences.getCustomTerminalCommand());
        useCustomFileBrowserProperty.setValue(initialExternalApplicationPreferences.useCustomFileBrowser());
        customFileBrowserCommandProperty.setValue(initialExternalApplicationPreferences.getCustomFileBrowserCommand());
    }

    public void storeSettings() {
        ExternalApplicationsPreferences externalPreferences = preferences.getExternalApplicationsPreferences();
        externalPreferences.setEMailSubject(eMailReferenceSubjectProperty.getValue());
        externalPreferences.setAutoOpenEmailAttachmentsFolder(autoOpenAttachedFoldersProperty.getValue());
        externalPreferences.setCiteCommand(citeCommandProperty.getValue());
        externalPreferences.setUseCustomTerminal(useCustomTerminalProperty.getValue());
        externalPreferences.setCustomTerminalCommand(customTerminalCommandProperty.getValue());
        externalPreferences.setUseCustomFileBrowser(useCustomFileBrowserProperty.getValue());
        externalPreferences.setCustomFileBrowserCommand(customFileBrowserCommandProperty.getValue());

        PushToApplicationPreferences pushPreferences = preferences.getPushToApplicationPreferences();
        pushPreferences.setActiveApplicationName(selectedPushToApplicationProperty.getValue().getDisplayName());
        pushPreferences.setCommandPaths(workingPushToApplicationPreferences.getCommandPaths());
        pushPreferences.setEmacsArguments(workingPushToApplicationPreferences.getEmacsArguments());
        pushPreferences.setVimServer(workingPushToApplicationPreferences.getVimServer());
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
