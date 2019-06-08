package org.jabref.gui.preferences;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.FieldName;
import org.jabref.model.metadata.FilePreferences;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.NewLineSeparator;

public class FileTabViewModel implements PreferenceTabViewModel {

    private BooleanProperty openLastStartupProperty = new SimpleBooleanProperty();
    private BooleanProperty backupOldFileProperty = new SimpleBooleanProperty();
    private StringProperty noWrapFilesProperty = new SimpleStringProperty("");
    private BooleanProperty resolveStringsBibTexProperty = new SimpleBooleanProperty();
    private BooleanProperty resolveStringsAllProperty = new SimpleBooleanProperty();
    private StringProperty resolveStringsExceptProperty = new SimpleStringProperty("");
    private final ListProperty<NewLineSeparator> newLineSeparatorListProperty = new SimpleListProperty<>();
    private final ObjectProperty<NewLineSeparator> selectedNewLineSeparatorProperty = new SimpleObjectProperty<>();
    private BooleanProperty alwaysReformatBibProperty = new SimpleBooleanProperty();

    private StringProperty mainFileDirProperty = new SimpleStringProperty("");
    private BooleanProperty useBibLocationAsPrimaryProperty = new SimpleBooleanProperty();
    private BooleanProperty autolinkFileStartsBibtexProperty = new SimpleBooleanProperty();
    private BooleanProperty autolinkFileExactBibtexProperty = new SimpleBooleanProperty();
    private BooleanProperty autolinkUseRegexProperty = new SimpleBooleanProperty();
    private StringProperty autolinkRegexKeyProperty = new SimpleStringProperty("");
    private BooleanProperty searchFilesOnOpenProperty = new SimpleBooleanProperty();
    private BooleanProperty openBrowseOnCreateProperty = new SimpleBooleanProperty();

    private BooleanProperty autosaveLocalLibraries = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final JabRefPreferences preferences;

    public FileTabViewModel(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        setValues();
    }

    public void setValues() {
        openLastStartupProperty.setValue(preferences.getBoolean(JabRefPreferences.OPEN_LAST_EDITED));
        backupOldFileProperty.setValue(preferences.getBoolean(JabRefPreferences.BACKUP));
        noWrapFilesProperty.setValue(preferences.get(JabRefPreferences.NON_WRAPPABLE_FIELDS));
        resolveStringsAllProperty.setValue(preferences.getBoolean(JabRefPreferences.RESOLVE_STRINGS_ALL_FIELDS)); // Flipped around
        resolveStringsBibTexProperty.setValue(!resolveStringsAllProperty.getValue());
        resolveStringsExceptProperty.setValue(preferences.get(JabRefPreferences.DO_NOT_RESOLVE_STRINGS_FOR));
        newLineSeparatorListProperty.setValue(FXCollections.observableArrayList(NewLineSeparator.values()));
        selectedNewLineSeparatorProperty.setValue(preferences.getNewLineSeparator());
        alwaysReformatBibProperty.setValue(preferences.getBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT));

        mainFileDirProperty.setValue(preferences.getAsOptional(FieldName.FILE + FilePreferences.DIR_SUFFIX).orElse(""));
        useBibLocationAsPrimaryProperty.setValue(preferences.getBoolean(JabRefPreferences.BIB_LOC_AS_PRIMARY_DIR));
        if (preferences.getBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY)) { // Flipped around
            autolinkUseRegexProperty.setValue(true);
        } else if (preferences.getBoolean(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY)) {
            autolinkFileExactBibtexProperty.setValue(true);
        } else {
            autolinkFileStartsBibtexProperty.setValue(true);
        }
        autolinkRegexKeyProperty.setValue(preferences.get(JabRefPreferences.AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY));
        searchFilesOnOpenProperty.setValue(preferences.getBoolean(JabRefPreferences.RUN_AUTOMATIC_FILE_SEARCH));
        openBrowseOnCreateProperty.setValue(preferences.getBoolean(JabRefPreferences.ALLOW_FILE_AUTO_OPEN_BROWSE));

        autosaveLocalLibraries.setValue(preferences.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE));
    }

    public void storeSettings() {
        preferences.putBoolean(JabRefPreferences.OPEN_LAST_EDITED, openLastStartupProperty.getValue());
        preferences.putBoolean(JabRefPreferences.BACKUP, backupOldFileProperty.getValue());
        if (!noWrapFilesProperty.getValue().trim().equals(preferences.get(JabRefPreferences.NON_WRAPPABLE_FIELDS))) {
            preferences.put(JabRefPreferences.NON_WRAPPABLE_FIELDS, noWrapFilesProperty.getValue());
        }
        preferences.putBoolean(JabRefPreferences.RESOLVE_STRINGS_ALL_FIELDS, resolveStringsAllProperty.getValue());
        preferences.put(JabRefPreferences.DO_NOT_RESOLVE_STRINGS_FOR, resolveStringsExceptProperty.getValue().trim());
        resolveStringsExceptProperty.setValue(preferences.get(JabRefPreferences.DO_NOT_RESOLVE_STRINGS_FOR));
        if (autolinkUseRegexProperty.getValue()) {
            preferences.put(JabRefPreferences.AUTOLINK_REG_EXP_SEARCH_EXPRESSION_KEY, autolinkRegexKeyProperty.getValue());
        }
        preferences.setNewLineSeparator(selectedNewLineSeparatorProperty.getValue());
        preferences.putBoolean(JabRefPreferences.REFORMAT_FILE_ON_SAVE_AND_EXPORT, alwaysReformatBibProperty.getValue());

        preferences.put(FieldName.FILE + FilePreferences.DIR_SUFFIX, mainFileDirProperty.getValue());
        preferences.putBoolean(JabRefPreferences.BIB_LOC_AS_PRIMARY_DIR, useBibLocationAsPrimaryProperty.getValue());
        preferences.putBoolean(JabRefPreferences.AUTOLINK_USE_REG_EXP_SEARCH_KEY, autolinkUseRegexProperty.getValue());
        preferences.putBoolean(JabRefPreferences.AUTOLINK_EXACT_KEY_ONLY, autolinkFileExactBibtexProperty.getValue());
        preferences.putBoolean(JabRefPreferences.RUN_AUTOMATIC_FILE_SEARCH, searchFilesOnOpenProperty.getValue());
        preferences.putBoolean(JabRefPreferences.ALLOW_FILE_AUTO_OPEN_BROWSE, openBrowseOnCreateProperty.getValue());

        preferences.putBoolean(JabRefPreferences.LOCAL_AUTO_SAVE, autosaveLocalLibraries.getValue());
    }

    public boolean validateSettings() {
        Path path = Paths.get(mainFileDirProperty.getValue());
        boolean valid = Files.exists(path) && Files.isDirectory(path);
        if (!valid) {
            dialogService.showErrorDialogAndWait(
                    String.format("%s -> %s %n %n %s: %n %s", Localization.lang("File"),
                            Localization.lang("Main file directory"), Localization.lang("Directory not found"), path));
        }
        return valid;
    }

    public void mainFileDirBrowse() {
        DirectoryDialogConfiguration dirDialogConfiguration =
                new DirectoryDialogConfiguration.Builder().withInitialDirectory(Paths.get(mainFileDirProperty.getValue())).build();
        dialogService.showDirectorySelectionDialog(dirDialogConfiguration)
                     .ifPresent(f -> mainFileDirProperty.setValue(f.toString()));
    }

    // General

    public BooleanProperty openLastStartupProperty() { return openLastStartupProperty; }

    public BooleanProperty backupOldFileProperty() { return backupOldFileProperty; }

    public StringProperty noWrapFilesProperty() { return noWrapFilesProperty; }

    public BooleanProperty resolveStringsBibTexProperty() { return resolveStringsBibTexProperty; }

    public BooleanProperty resolveStringsAllProperty() { return resolveStringsAllProperty; }

    public StringProperty resolvStringsExceptProperty() { return resolveStringsExceptProperty; }

    public ListProperty<NewLineSeparator> newLineSeparatorListProperty() { return newLineSeparatorListProperty; }

    public ObjectProperty<NewLineSeparator> selectedNewLineSeparatorProperty() { return selectedNewLineSeparatorProperty; }

    public BooleanProperty alwaysReformatBibProperty() { return alwaysReformatBibProperty; }

    // External file links

    public StringProperty mainFileDirProperty() { return mainFileDirProperty; }

    public BooleanProperty useBibLocationAsPrimaryProperty() { return useBibLocationAsPrimaryProperty; }

    public BooleanProperty autolinkFileStartsBibtexProperty() { return autolinkFileStartsBibtexProperty; }

    public BooleanProperty autolinkFileExactBibtexProperty() { return autolinkFileExactBibtexProperty; }

    public BooleanProperty autolinkUseRegexProperty() { return autolinkUseRegexProperty; }

    public StringProperty autolinkRegexKeyProperty() { return autolinkRegexKeyProperty; }

    public BooleanProperty searchFilesOnOpenProperty() { return searchFilesOnOpenProperty; }

    public BooleanProperty openBrowseOnCreateProperty() { return openBrowseOnCreateProperty; }

    // Autosave

    public BooleanProperty autosaveLocalLibrariesProperty() { return autosaveLocalLibraries; }
}

