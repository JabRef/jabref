package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.DialogService;
import org.jabref.preferences.JabRefPreferences;

public class TableTabViewModel implements PreferenceTabViewModel {

    private BooleanProperty autoResizeNameProperty = new SimpleBooleanProperty();
    private BooleanProperty namesNatbibProperty = new SimpleBooleanProperty();
    private BooleanProperty nameAsIsProperty = new SimpleBooleanProperty();
    private BooleanProperty nameFirstLastProperty = new SimpleBooleanProperty();
    private BooleanProperty nameLastFirstProperty = new SimpleBooleanProperty();
    private BooleanProperty abbreviationDisabledProperty = new SimpleBooleanProperty();
    private BooleanProperty abbreviationEnabledProperty = new SimpleBooleanProperty();
    private BooleanProperty abbreviationLastNameOnlyProperty = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final JabRefPreferences preferences;

    public TableTabViewModel(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
    }

    @Override
    public void setValues() {
        autoResizeNameProperty.setValue(preferences.getBoolean(JabRefPreferences.AUTO_RESIZE_MODE));

        if (preferences.getBoolean(JabRefPreferences.NAMES_NATBIB)) {
            namesNatbibProperty.setValue(true);
            nameAsIsProperty.setValue(false);
            nameFirstLastProperty.setValue(false);
            nameLastFirstProperty.setValue(false);
        } else if (preferences.getBoolean(JabRefPreferences.NAMES_AS_IS)) {
            namesNatbibProperty.setValue(false);
            nameAsIsProperty.setValue(true);
            nameFirstLastProperty.setValue(false);
            nameLastFirstProperty.setValue(false);
        } else if (preferences.getBoolean(JabRefPreferences.NAMES_FIRST_LAST)) {
            namesNatbibProperty.setValue(false);
            nameAsIsProperty.setValue(false);
            nameFirstLastProperty.setValue(true);
            nameLastFirstProperty.setValue(false);
        } else {
            namesNatbibProperty.setValue(false);
            nameAsIsProperty.setValue(false);
            nameFirstLastProperty.setValue(false);
            nameLastFirstProperty.setValue(true);
        }

        if (preferences.getBoolean(JabRefPreferences.ABBR_AUTHOR_NAMES)) {
            abbreviationEnabledProperty.setValue(true);
        } else if (preferences.getBoolean(JabRefPreferences.NAMES_LAST_ONLY)) {
            abbreviationLastNameOnlyProperty.setValue(true);
        } else {
            abbreviationDisabledProperty.setValue(true);
        }
    }

    @Override
    public void storeSettings() {
        preferences.putBoolean(JabRefPreferences.AUTO_RESIZE_MODE, autoResizeNameProperty.getValue());

        preferences.putBoolean(JabRefPreferences.NAMES_NATBIB, namesNatbibProperty.getValue());
        preferences.putBoolean(JabRefPreferences.NAMES_AS_IS, nameAsIsProperty.getValue());
        preferences.putBoolean(JabRefPreferences.NAMES_FIRST_LAST, nameFirstLastProperty.getValue());
        preferences.putBoolean(JabRefPreferences.ABBR_AUTHOR_NAMES, abbreviationEnabledProperty.getValue());
        preferences.putBoolean(JabRefPreferences.NAMES_LAST_ONLY, abbreviationLastNameOnlyProperty.getValue());
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public List<String> getRestartWarnings() {
        return new ArrayList<>();
    }

    public BooleanProperty autoResizeNameProperty() { return autoResizeNameProperty; }

    public BooleanProperty namesNatbibProperty() { return namesNatbibProperty; }

    public BooleanProperty nameAsIsProperty() { return nameAsIsProperty; }

    public BooleanProperty nameFirstLastProperty() { return nameFirstLastProperty; }

    public BooleanProperty nameLastFirstProperty() { return nameLastFirstProperty; }

    public BooleanProperty abbreviationDisabledProperty() { return abbreviationDisabledProperty; }

    public BooleanProperty abbreviationEnabledProperty() { return abbreviationEnabledProperty; }

    public BooleanProperty abbreviationLastNameOnlyProperty() { return abbreviationLastNameOnlyProperty; }
}
