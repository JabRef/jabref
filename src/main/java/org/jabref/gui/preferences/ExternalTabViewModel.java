package org.jabref.gui.preferences;

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
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiletype.EditExternalFileTypesAction;
import org.jabref.gui.push.PushToApplication;
import org.jabref.gui.push.PushToApplicationSettings;
import org.jabref.gui.push.PushToApplicationsManager;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.JabRefPreferences;

public class ExternalTabViewModel implements PreferenceTabViewModel {

    private final StringProperty eMailReferenceSubjectProperty = new SimpleStringProperty("");
    private final BooleanProperty autoOpenAttachedFoldersProperty = new SimpleBooleanProperty();

    private final ListProperty<PushToApplication> pushToApplicationsListProperty = new SimpleListProperty<>();
    private final ObjectProperty<PushToApplication> selectedPushToApplicationProperty = new SimpleObjectProperty<>();
    private final StringProperty citeCommandProperty = new SimpleStringProperty("");

    private final BooleanProperty useTerminalDefaultProperty = new SimpleBooleanProperty();
    private final BooleanProperty useTerminalSpecialProperty = new SimpleBooleanProperty();
    private final StringProperty useTerminalCommandProperty = new SimpleStringProperty("");

    private final BooleanProperty usePDFAcrobatProperty = new SimpleBooleanProperty();
    private final StringProperty usePDFAcrobatCommandProperty = new SimpleStringProperty("");
    private final BooleanProperty usePDFSumatraProperty = new SimpleBooleanProperty();
    private final StringProperty usePDFSumatraCommandProperty = new SimpleStringProperty("");

    private final BooleanProperty useFileBrowserDefaultProperty = new SimpleBooleanProperty();
    private final BooleanProperty useFileBrowserSpecialProperty = new SimpleBooleanProperty();
    private final StringProperty useFileBrowserSpecialCommandProperty = new SimpleStringProperty("");

    private final DialogService dialogService;
    private final JabRefPreferences preferences;
    private final JabRefFrame frame;

    private final FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder().build();

    public ExternalTabViewModel(DialogService dialogService, JabRefPreferences preferences, JabRefFrame frame) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.frame = frame;
    }

    public void setValues() {

        eMailReferenceSubjectProperty.setValue(preferences.get(JabRefPreferences.EMAIL_SUBJECT));
        autoOpenAttachedFoldersProperty.setValue(preferences.getBoolean(JabRefPreferences.OPEN_FOLDERS_OF_ATTACHED_FILES));

        pushToApplicationsListProperty.setValue(FXCollections.observableArrayList(frame.getPushToApplicationsManager().getApplications()));
        selectedPushToApplicationProperty.setValue(preferences.getActivePushToApplication(frame.getPushToApplicationsManager()));
        citeCommandProperty.setValue(preferences.get(JabRefPreferences.CITE_COMMAND));

        useTerminalDefaultProperty.setValue(preferences.getBoolean(JabRefPreferences.USE_DEFAULT_CONSOLE_APPLICATION));
        useTerminalCommandProperty.setValue(preferences.get(JabRefPreferences.CONSOLE_COMMAND));
        useTerminalSpecialProperty.setValue(!preferences.getBoolean(JabRefPreferences.USE_DEFAULT_CONSOLE_APPLICATION));

        usePDFAcrobatCommandProperty.setValue(preferences.get(JabRefPreferences.ADOBE_ACROBAT_COMMAND));
        if (OS.WINDOWS) {
            usePDFSumatraCommandProperty.setValue(preferences.get(JabRefPreferences.SUMATRA_PDF_COMMAND));

            if (preferences.get(JabRefPreferences.USE_PDF_READER).equals(usePDFAcrobatCommandProperty.getValue())) {
                usePDFAcrobatProperty.setValue(true);
            } else if (preferences.get(JabRefPreferences.USE_PDF_READER).equals(usePDFSumatraCommandProperty.getValue())) {
                usePDFSumatraProperty.setValue(true);
            }
        }

        useFileBrowserDefaultProperty.setValue(preferences.getBoolean(JabRefPreferences.USE_DEFAULT_FILE_BROWSER_APPLICATION));
        useFileBrowserSpecialProperty.setValue(!preferences.getBoolean(JabRefPreferences.USE_DEFAULT_FILE_BROWSER_APPLICATION));
        useFileBrowserSpecialCommandProperty.setValue(preferences.get(JabRefPreferences.FILE_BROWSER_COMMAND));
    }

    public void storeSettings() {
        preferences.put(JabRefPreferences.EMAIL_SUBJECT, eMailReferenceSubjectProperty.getValue());
        preferences.putBoolean(JabRefPreferences.OPEN_FOLDERS_OF_ATTACHED_FILES, autoOpenAttachedFoldersProperty.getValue());

        preferences.setActivePushToApplication(selectedPushToApplicationProperty.getValue(), frame.getPushToApplicationsManager());
        preferences.put(JabRefPreferences.CITE_COMMAND, citeCommandProperty.getValue());

        preferences.putBoolean(JabRefPreferences.USE_DEFAULT_CONSOLE_APPLICATION, useTerminalDefaultProperty.getValue());
        preferences.put(JabRefPreferences.CONSOLE_COMMAND, useTerminalCommandProperty.getValue());

        preferences.put(JabRefPreferences.ADOBE_ACROBAT_COMMAND, usePDFAcrobatCommandProperty.getValue());
        if (OS.WINDOWS) {
            preferences.put(JabRefPreferences.SUMATRA_PDF_COMMAND, usePDFSumatraCommandProperty.getValue());
        }
        if (usePDFAcrobatProperty.getValue()) {
            preferences.put(JabRefPreferences.USE_PDF_READER, usePDFAcrobatCommandProperty.getValue());
        } else if (usePDFSumatraProperty.getValue()) {
            preferences.put(JabRefPreferences.USE_PDF_READER, usePDFSumatraCommandProperty.getValue());
        }

        preferences.putBoolean(JabRefPreferences.USE_DEFAULT_FILE_BROWSER_APPLICATION, useFileBrowserDefaultProperty.getValue());
        if (StringUtil.isNotBlank(useFileBrowserSpecialCommandProperty.getValue())) {
            preferences.put(JabRefPreferences.FILE_BROWSER_COMMAND, useFileBrowserSpecialCommandProperty.getValue());
        } else {
            preferences.putBoolean(JabRefPreferences.USE_DEFAULT_FILE_BROWSER_APPLICATION, true); //default if no command specified
        }
    }

    public boolean validateSettings() {
        return true;
    }

    @Override
    public List<String> getRestartWarnings() { return new ArrayList<>(); }

    public void pushToApplicationSettings() {
        PushToApplicationsManager manager = frame.getPushToApplicationsManager();
        PushToApplication selectedApplication = selectedPushToApplicationProperty.getValue();
        PushToApplicationSettings settings = manager.getSettings(selectedApplication);

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

    public void manageExternalFileTypes() {
        new EditExternalFileTypesAction().execute();
    }

    public void useTerminalCommandBrowse() {
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file -> useTerminalCommandProperty.setValue(file.toAbsolutePath().toString()));
    }

    public void usePDFAcrobatCommandBrowse() {
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file -> usePDFAcrobatCommandProperty.setValue(file.toAbsolutePath().toString()));
    }

    public void usePDFSumatraCommandBrowse() {
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file -> usePDFSumatraCommandProperty.setValue(file.toAbsolutePath().toString()));
    }

    public void useFileBrowserSpecialCommandBrowse() {
        dialogService.showFileOpenDialog(fileDialogConfiguration).ifPresent(file -> useFileBrowserSpecialCommandProperty.setValue(file.toAbsolutePath().toString()));
    }

    // EMail

    public StringProperty eMailReferenceSubjectProperty() { return this.eMailReferenceSubjectProperty; }

    public BooleanProperty autoOpenAttachedFoldersProperty() { return this.autoOpenAttachedFoldersProperty; }

    // Push-To-Application

    public ListProperty<PushToApplication> pushToApplicationsListProperty() { return this.pushToApplicationsListProperty; }

    public ObjectProperty<PushToApplication> selectedPushToApplication() { return this.selectedPushToApplicationProperty; }

    public StringProperty citeCommandProperty() { return this.citeCommandProperty; }

    // Open console

    public BooleanProperty useTerminalDefaultProperty() { return this.useTerminalDefaultProperty; }

    public BooleanProperty useTerminalSpecialProperty() { return this.useTerminalSpecialProperty; }

    public StringProperty useTerminalCommandProperty() { return this.useTerminalCommandProperty; }

    // Open PDF

    public BooleanProperty usePDFAcrobatProperty() { return this.usePDFAcrobatProperty; }

    public StringProperty usePDFAcrobatCommandProperty() { return this.usePDFAcrobatCommandProperty; }

    public BooleanProperty usePDFSumatraProperty() { return this.usePDFSumatraProperty; }

    public StringProperty usePDFSumatraCommandProperty() { return this.usePDFSumatraCommandProperty; }

    // Open File Browser

    public BooleanProperty useFileBrowserDefaultProperty() { return this.useFileBrowserDefaultProperty; }

    public BooleanProperty useFileBrowserSpecialProperty() { return this.useFileBrowserSpecialProperty; }

    public StringProperty useFileBrowserSpecialCommandProperty() { return this.useFileBrowserSpecialCommandProperty; }
}
