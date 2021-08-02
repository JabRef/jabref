package org.jabref.gui.preferences.importexport;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.importer.importsettings.ImportSettingsPreferences;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.preferences.PreferencesService;

public class ImportExportTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty generateKeyOnImportProperty = new SimpleBooleanProperty();

    private final BooleanProperty useCustomDOIProperty = new SimpleBooleanProperty();
    private final StringProperty useCustomDOINameProperty = new SimpleStringProperty("");

    private final PreferencesService preferencesService;

    private final DOIPreferences initialDOIPreferences;
    private final ImportSettingsPreferences initialImportSettingsPreferences;

    public ImportExportTabViewModel(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
        this.initialImportSettingsPreferences = preferencesService.getImportSettingsPreferences();
        this.initialDOIPreferences = preferencesService.getDOIPreferences();
    }

    @Override
    public void setValues() {
        generateKeyOnImportProperty.setValue(initialImportSettingsPreferences.generateNewKeyOnImport());
        useCustomDOIProperty.setValue(initialDOIPreferences.isUseCustom());
        useCustomDOINameProperty.setValue(initialDOIPreferences.getDefaultBaseURI());
    }

    @Override
    public void storeSettings() {
        preferencesService.storeImportSettingsPreferences(new ImportSettingsPreferences(
                generateKeyOnImportProperty.getValue()));

        preferencesService.storeDOIPreferences(new DOIPreferences(
                useCustomDOIProperty.getValue(),
                useCustomDOINameProperty.getValue().trim()));
    }

    public BooleanProperty generateKeyOnImportProperty() {
        return generateKeyOnImportProperty;
    }

    public BooleanProperty useCustomDOIProperty() {
        return this.useCustomDOIProperty;
    }

    public StringProperty useCustomDOINameProperty() {
        return this.useCustomDOINameProperty;
    }
}
