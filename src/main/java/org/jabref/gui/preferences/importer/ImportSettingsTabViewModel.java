package org.jabref.gui.preferences.importer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.importer.importsettings.ImportSettingsPreferences;
import org.jabref.preferences.PreferencesService;

public class ImportSettingsTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty generateKeyOnImportProperty = new SimpleBooleanProperty();

    private final PreferencesService preferences;
    private final ImportSettingsPreferences initialImportSettingsPreferences;

    public ImportSettingsTabViewModel(PreferencesService preferences) {
        this.preferences = preferences;
        this.initialImportSettingsPreferences = preferences.getImportSettingsPreferences();
    }

    @Override
    public void setValues() {
        generateKeyOnImportProperty.setValue(initialImportSettingsPreferences.generateNewKeyOnImport());
    }

    @Override
    public void storeSettings() {
        ImportSettingsPreferences newImportSettingsPreferences = new ImportSettingsPreferences(
                generateKeyOnImportProperty.getValue()
        );
        preferences.storeImportSettingsPreferences(newImportSettingsPreferences);
    }

    public BooleanProperty generateKeyOnImportProperty() {
        return generateKeyOnImportProperty;
    }

}
