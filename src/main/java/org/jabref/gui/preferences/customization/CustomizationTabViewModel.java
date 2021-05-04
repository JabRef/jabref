package org.jabref.gui.preferences.customization;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.preferences.PreferencesService;

public class CustomizationTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty useCustomDOIProperty = new SimpleBooleanProperty();
    private final StringProperty useCustomDOINameProperty = new SimpleStringProperty("");

    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final DOIPreferences initialDOIPreferences;

    public CustomizationTabViewModel(DialogService dialogService, PreferencesService preferencesService) {
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.initialDOIPreferences = preferencesService.getDOIPreferences();
    }

    @Override
    public void setValues() {
        useCustomDOIProperty.setValue(initialDOIPreferences.isUseCustom());
        useCustomDOINameProperty.setValue(initialDOIPreferences.getDefaultBaseURI());
    }

    @Override
    public void storeSettings() {
        preferencesService.storeDOIPreferences(new DOIPreferences(
                useCustomDOIProperty.getValue(),
                useCustomDOINameProperty.getValue().trim()));
    }

    public BooleanProperty useCustomDOIProperty() {
        return this.useCustomDOIProperty;
    }

    public StringProperty useCustomDOINameProperty() {
        return this.useCustomDOINameProperty;
    }
}
