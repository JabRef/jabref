package org.jabref.gui.preferences.general;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
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
import javafx.collections.transformation.SortedList;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.GeneralPreferences;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.TelemetryPreferences;

public class GeneralTabViewModel implements PreferenceTabViewModel {
    private final ListProperty<Language> languagesListProperty = new SimpleListProperty<>();
    private final ObjectProperty<Language> selectedLanguageProperty = new SimpleObjectProperty<>();
    private final ListProperty<BibDatabaseMode> bibliographyModeListProperty = new SimpleListProperty<>();
    private final ObjectProperty<BibDatabaseMode> selectedBiblatexModeProperty = new SimpleObjectProperty<>();

    private final BooleanProperty inspectionWarningDuplicateProperty = new SimpleBooleanProperty();
    private final BooleanProperty confirmDeleteProperty = new SimpleBooleanProperty();
    private final BooleanProperty memoryStickModeProperty = new SimpleBooleanProperty();
    private final BooleanProperty collectTelemetryProperty = new SimpleBooleanProperty();
    private final BooleanProperty openLastStartupProperty = new SimpleBooleanProperty();
    private final BooleanProperty showAdvancedHintsProperty = new SimpleBooleanProperty();

    private final BooleanProperty createBackupProperty = new SimpleBooleanProperty();
    private final StringProperty backupDirectoryProperty = new SimpleStringProperty("");

    private final DialogService dialogService;
    private final GeneralPreferences generalPreferences;
    private final TelemetryPreferences telemetryPreferences;
    private final FilePreferences filePreferences;

    private final List<String> restartWarning = new ArrayList<>();

    @SuppressWarnings("ReturnValueIgnored")
    public GeneralTabViewModel(PreferencesService preferencesService, DialogService dialogService) {
        this.dialogService = dialogService;
        this.generalPreferences = preferencesService.getGeneralPreferences();
        this.telemetryPreferences = preferencesService.getTelemetryPreferences();
        this.filePreferences = preferencesService.getFilePreferences();
    }

    @Override
    public void setValues() {
        languagesListProperty.setValue(new SortedList<>(FXCollections.observableArrayList(Language.values()), Comparator.comparing(Language::getDisplayName)));
        selectedLanguageProperty.setValue(generalPreferences.getLanguage());

        bibliographyModeListProperty.setValue(FXCollections.observableArrayList(BibDatabaseMode.values()));
        selectedBiblatexModeProperty.setValue(generalPreferences.getDefaultBibDatabaseMode());

        inspectionWarningDuplicateProperty.setValue(generalPreferences.warnAboutDuplicatesInInspection());
        confirmDeleteProperty.setValue(generalPreferences.shouldConfirmDelete());
        memoryStickModeProperty.setValue(generalPreferences.isMemoryStickMode());
        collectTelemetryProperty.setValue(telemetryPreferences.shouldCollectTelemetry());
        openLastStartupProperty.setValue(generalPreferences.shouldOpenLastEdited());
        showAdvancedHintsProperty.setValue(generalPreferences.shouldShowAdvancedHints());

        createBackupProperty.setValue(filePreferences.shouldCreateBackup());
        backupDirectoryProperty.setValue(filePreferences.getBackupDirectory().toString());
    }

    @Override
    public void storeSettings() {
        Language newLanguage = selectedLanguageProperty.getValue();
        if (newLanguage != generalPreferences.getLanguage()) {
            generalPreferences.setLanguage(newLanguage);
            Localization.setLanguage(newLanguage);
            restartWarning.add(Localization.lang("Changed language") + ": " + newLanguage.getDisplayName());
        }

        if (generalPreferences.isMemoryStickMode() && !memoryStickModeProperty.getValue()) {
            dialogService.showInformationDialogAndWait(Localization.lang("Memory stick mode"),
                    Localization.lang("To disable the memory stick mode"
                            + " rename or remove the jabref.xml file in the same folder as JabRef."));
        }

        generalPreferences.setDefaultBibDatabaseMode(selectedBiblatexModeProperty.getValue());
        generalPreferences.setWarnAboutDuplicatesInInspection(inspectionWarningDuplicateProperty.getValue());
        generalPreferences.setConfirmDelete(confirmDeleteProperty.getValue());
        generalPreferences.setMemoryStickMode(memoryStickModeProperty.getValue());
        generalPreferences.setOpenLastEdited(openLastStartupProperty.getValue());
        generalPreferences.setShowAdvancedHints(showAdvancedHintsProperty.getValue());

        telemetryPreferences.setCollectTelemetry(collectTelemetryProperty.getValue());

        filePreferences.createBackupProperty().setValue(createBackupProperty.getValue());
        filePreferences.backupDirectoryProperty().setValue(Path.of(backupDirectoryProperty.getValue()));
    }

    @Override
    public List<String> getRestartWarnings() {
        return restartWarning;
    }

    // General

    public ListProperty<Language> languagesListProperty() {
        return this.languagesListProperty;
    }

    public ObjectProperty<Language> selectedLanguageProperty() {
        return this.selectedLanguageProperty;
    }

    public ListProperty<BibDatabaseMode> biblatexModeListProperty() {
        return this.bibliographyModeListProperty;
    }

    public ObjectProperty<BibDatabaseMode> selectedBiblatexModeProperty() {
        return this.selectedBiblatexModeProperty;
    }

    public BooleanProperty inspectionWarningDuplicateProperty() {
        return this.inspectionWarningDuplicateProperty;
    }

    public BooleanProperty confirmDeleteProperty() {
        return this.confirmDeleteProperty;
    }

    public BooleanProperty memoryStickModeProperty() {
        return this.memoryStickModeProperty;
    }

    public BooleanProperty collectTelemetryProperty() {
        return this.collectTelemetryProperty;
    }

    public BooleanProperty openLastStartupProperty() {
        return openLastStartupProperty;
    }

    public BooleanProperty showAdvancedHintsProperty() {
        return this.showAdvancedHintsProperty;
    }

    public BooleanProperty createBackupProperty() {
        return this.createBackupProperty;
    }

    public StringProperty backupDirectoryProperty() {
        return this.backupDirectoryProperty;
    }

    public void backupFileDirBrowse() {
        DirectoryDialogConfiguration dirDialogConfiguration =
                new DirectoryDialogConfiguration.Builder().withInitialDirectory(Path.of(backupDirectoryProperty().getValue())).build();
        dialogService.showDirectorySelectionDialog(dirDialogConfiguration)
                     .ifPresent(dir -> backupDirectoryProperty.setValue(dir.toString()));
    }
}
