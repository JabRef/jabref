package org.jabref.gui.preferences.external;

import java.util.ArrayList;
import java.util.List;

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
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.push.GuiPushToApplication;
import org.jabref.gui.push.GuiPushToApplicationSettings;
import org.jabref.gui.push.GuiPushToApplications;
import org.jabref.gui.push.GuiPushToEmacs;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.validation.ValidationConstraints;
import org.jabref.gui.validation.ValidationMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.push.CitationCommandString;
import org.jabref.logic.push.PushToApplicationPreferences;
import org.jabref.logic.util.strings.StringUtil;

import org.jfxcore.validation.property.ConstrainedStringProperty;
import org.jfxcore.validation.property.ReadOnlyConstrainedProperty;
import org.jfxcore.validation.property.SimpleConstrainedStringProperty;

public class ExternalTabViewModel implements PreferenceTabViewModel {

    private final StringProperty eMailReferenceSubjectProperty = new SimpleStringProperty("");
    private final BooleanProperty autoOpenAttachedFoldersProperty = new SimpleBooleanProperty();
    private final ListProperty<GuiPushToApplication> pushToApplicationsListProperty = new SimpleListProperty<>();
    private final ObjectProperty<GuiPushToApplication> selectedPushToApplicationProperty = new SimpleObjectProperty<>();
    private final ConstrainedStringProperty<ValidationMessage> citeCommandProperty = new SimpleConstrainedStringProperty<>(
            "",
            ValidationConstraints.predicate(
                    input -> {
                        int indexKey1 = input.indexOf(CitationCommandString.CITE_KEY1);
                        int indexKey2 = input.indexOf(CitationCommandString.CITE_KEY2);
                        return indexKey1 >= 0 && indexKey2 >= 0 && indexKey2 >= (indexKey1 + CitationCommandString.CITE_KEY1.length());
                    },
                    ValidationMessage.warning(Localization.lang("The cite command should contain '%0' and '%1'.", CitationCommandString.CITE_KEY1, CitationCommandString.CITE_KEY2))));
    private final BooleanProperty useCustomTerminalProperty = new SimpleBooleanProperty();
    private final ConstrainedStringProperty<ValidationMessage> customTerminalCommandProperty = new SimpleConstrainedStringProperty<>(
            "",
            ValidationConstraints.predicate(
                    input -> !StringUtil.isNullOrEmpty(input),
                    ValidationMessage.error("%s > %s %n %n %s".formatted(
                            Localization.lang("External programs"),
                            Localization.lang("Custom applications"),
                            Localization.lang("Please specify a terminal application.")))));
    private final BooleanProperty useCustomFileBrowserProperty = new SimpleBooleanProperty();
    private final ConstrainedStringProperty<ValidationMessage> customFileBrowserCommandProperty = new SimpleConstrainedStringProperty<>(
            "",
            ValidationConstraints.predicate(
                    input -> !StringUtil.isNullOrEmpty(input),
                    ValidationMessage.error("%s > %s %n %n %s".formatted(
                            Localization.lang("External programs"),
                            Localization.lang("Custom applications"),
                            Localization.lang("Please specify a file browser.")))));
    private final StringProperty kindleEmailProperty = new SimpleStringProperty("");

    private final DialogService dialogService;
    private final GuiPreferences preferences;

    private final FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder().build();

    private final ExternalApplicationsPreferences initialExternalApplicationPreferences;
    private final PushToApplicationPreferences initialPushToApplicationPreferences;
    private final PushToApplicationPreferences workingPushToApplicationPreferences;

    public ExternalTabViewModel(DialogService dialogService, GuiPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.initialExternalApplicationPreferences = preferences.getExternalApplicationsPreferences();
        this.initialPushToApplicationPreferences = preferences.getPushToApplicationPreferences();
        this.workingPushToApplicationPreferences = PushToApplicationPreferences.getDefault();
        copyPushPreferences(workingPushToApplicationPreferences, initialPushToApplicationPreferences);
    }

    @Override
    public void setValues() {
        copyPushPreferences(workingPushToApplicationPreferences, preferences.getPushToApplicationPreferences());

        eMailReferenceSubjectProperty.setValue(initialExternalApplicationPreferences.getEmailSubject());
        autoOpenAttachedFoldersProperty.setValue(initialExternalApplicationPreferences.shouldAutoOpenEmailAttachmentsFolder());

        pushToApplicationsListProperty.setValue(
                FXCollections.observableArrayList(GuiPushToApplications.getAllGUIApplications(dialogService, preferences.getPushToApplicationPreferences())));
        selectedPushToApplicationProperty.setValue(
                GuiPushToApplications.getGUIApplicationByName(initialPushToApplicationPreferences.getActiveApplicationName(), dialogService, preferences.getPushToApplicationPreferences())
                                     .orElseGet(() -> new GuiPushToEmacs(dialogService, preferences.getPushToApplicationPreferences())));

        citeCommandProperty.setValue(initialPushToApplicationPreferences.getCiteCommand().toString());

        useCustomTerminalProperty.setValue(initialExternalApplicationPreferences.useCustomTerminal());
        customTerminalCommandProperty.setValue(initialExternalApplicationPreferences.getCustomTerminalCommand());
        useCustomFileBrowserProperty.setValue(initialExternalApplicationPreferences.useCustomFileBrowser());
        customFileBrowserCommandProperty.setValue(initialExternalApplicationPreferences.getCustomFileBrowserCommand());
        kindleEmailProperty.setValue(initialExternalApplicationPreferences.getKindleEmail());
    }

    @Override
    public void storeSettings() {
        ExternalApplicationsPreferences externalPreferences = preferences.getExternalApplicationsPreferences();
        externalPreferences.setEMailSubject(eMailReferenceSubjectProperty.getValue());
        externalPreferences.setAutoOpenEmailAttachmentsFolder(autoOpenAttachedFoldersProperty.getValue());
        externalPreferences.setUseCustomTerminal(useCustomTerminalProperty.getValue());
        externalPreferences.setCustomTerminalCommand(customTerminalCommandProperty.getValue());
        externalPreferences.setUseCustomFileBrowser(useCustomFileBrowserProperty.getValue());
        externalPreferences.setCustomFileBrowserCommand(customFileBrowserCommandProperty.getValue());
        externalPreferences.setKindleEmail(kindleEmailProperty.getValue());

        PushToApplicationPreferences pushPreferences = preferences.getPushToApplicationPreferences();
        pushPreferences.setActiveApplicationName(selectedPushToApplicationProperty.getValue().getDisplayName());
        pushPreferences.setCommandPaths(workingPushToApplicationPreferences.getCommandPaths());
        pushPreferences.setEmacsArguments(workingPushToApplicationPreferences.getEmacsArguments());
        pushPreferences.setVimServer(workingPushToApplicationPreferences.getVimServer());
        pushPreferences.setCiteCommand(CitationCommandString.from(citeCommandProperty.getValue()));
    }

    /// Copies all push-to-application settings from {@code source} into the existing {@code target} instance.
    private static void copyPushPreferences(PushToApplicationPreferences target, PushToApplicationPreferences source) {
        target.setActiveApplicationName(source.getActiveApplicationName());
        target.setCommandPaths(source.getCommandPaths());
        target.setEmacsArguments(source.getEmacsArguments());
        target.setVimServer(source.getVimServer());
        target.setCiteCommand(source.getCiteCommand());
    }

    @Override
    public boolean validateSettings() {
        List<ReadOnlyConstrainedProperty<?, ValidationMessage>> fieldsToCheck = new ArrayList<>();

        if (useCustomTerminalProperty.getValue()) {
            fieldsToCheck.add(customTerminalCommandProperty);
        }

        if (useCustomFileBrowserProperty.getValue()) {
            fieldsToCheck.add(customFileBrowserCommandProperty);
        }

        fieldsToCheck.add(citeCommandProperty);

        for (ReadOnlyConstrainedProperty<?, ValidationMessage> field : fieldsToCheck) {
            if (field.isInvalid()) {
                dialogService.showErrorDialogAndWait(field.getDiagnostics().invalidSubList().getFirst().message());
                return false;
            }
        }
        return true;
    }

    public void pushToApplicationSettings() {
        GuiPushToApplication selectedApplication = selectedPushToApplicationProperty.getValue();
        GuiPushToApplicationSettings settings = selectedApplication.getSettings(
                selectedApplication,
                dialogService,
                preferences.getFilePreferences(),
                workingPushToApplicationPreferences);

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

    public StringProperty kindleEmailProperty() {
        return this.kindleEmailProperty;
    }

    public BooleanProperty autoOpenAttachedFoldersProperty() {
        return this.autoOpenAttachedFoldersProperty;
    }

    // Push-To-Application

    public ListProperty<GuiPushToApplication> pushToApplicationsListProperty() {
        return this.pushToApplicationsListProperty;
    }

    public ObjectProperty<GuiPushToApplication> selectedPushToApplication() {
        return this.selectedPushToApplicationProperty;
    }

    public ConstrainedStringProperty<ValidationMessage> citeCommandProperty() {
        return this.citeCommandProperty;
    }

    public BooleanProperty useCustomTerminalProperty() {
        return this.useCustomTerminalProperty;
    }

    public ConstrainedStringProperty<ValidationMessage> customTerminalCommandProperty() {
        return this.customTerminalCommandProperty;
    }

    // Open File Browser

    public BooleanProperty useCustomFileBrowserProperty() {
        return this.useCustomFileBrowserProperty;
    }

    public ConstrainedStringProperty<ValidationMessage> customFileBrowserCommandProperty() {
        return this.customFileBrowserCommandProperty;
    }

    public void resetCiteCommandToDefault() {
        this.citeCommandProperty.setValue(PushToApplicationPreferences.getDefault().getCiteCommand().toString());
    }
}
