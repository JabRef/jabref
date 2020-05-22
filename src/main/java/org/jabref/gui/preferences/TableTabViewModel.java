package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.maintable.MainTableNameFormatPreferences;
import org.jabref.preferences.PreferencesService;

import static org.jabref.gui.maintable.MainTableNameFormatPreferences.AbbreviationStyle;
import static org.jabref.gui.maintable.MainTableNameFormatPreferences.DisplayStyle;

public class TableTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty namesNatbibProperty = new SimpleBooleanProperty();
    private final BooleanProperty nameAsIsProperty = new SimpleBooleanProperty();
    private final BooleanProperty nameFirstLastProperty = new SimpleBooleanProperty();
    private final BooleanProperty nameLastFirstProperty = new SimpleBooleanProperty();
    private final BooleanProperty abbreviationDisabledProperty = new SimpleBooleanProperty();
    private final BooleanProperty abbreviationEnabledProperty = new SimpleBooleanProperty();
    private final BooleanProperty abbreviationLastNameOnlyProperty = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final PreferencesService preferences;

    public TableTabViewModel(DialogService dialogService, PreferencesService preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
    }

    @Override
    public void setValues() {
        MainTableNameFormatPreferences initialPreferences = preferences.getMainTableNameFormatPreferences();

        switch (initialPreferences.getDisplayStyle()) {
            case NATBIB:
                namesNatbibProperty.setValue(true);
                nameAsIsProperty.setValue(false);
                nameFirstLastProperty.setValue(false);
                nameLastFirstProperty.setValue(false);
                break;
            case AS_IS:
                namesNatbibProperty.setValue(false);
                nameAsIsProperty.setValue(true);
                nameFirstLastProperty.setValue(false);
                nameLastFirstProperty.setValue(false);
                break;
            case FIRSTNAME_LASTNAME:
                namesNatbibProperty.setValue(false);
                nameAsIsProperty.setValue(false);
                nameFirstLastProperty.setValue(true);
                nameLastFirstProperty.setValue(false);
                break;
            default:
            case LASTNAME_FIRSTNAME:
                namesNatbibProperty.setValue(false);
                nameAsIsProperty.setValue(false);
                nameFirstLastProperty.setValue(false);
                nameLastFirstProperty.setValue(true);
                break;
        }

        switch (initialPreferences.getAbbreviationStyle()) {
            case FULL:
                abbreviationEnabledProperty.setValue(true);
                break;
            case LASTNAME_ONLY:
                abbreviationLastNameOnlyProperty.setValue(true);
                break;
            default:
            case NONE:
                abbreviationDisabledProperty.setValue(true);
        }
    }

    @Override
    public void storeSettings() {
        DisplayStyle displayStyle = DisplayStyle.LASTNAME_FIRSTNAME;

        if (namesNatbibProperty.getValue()) {
            displayStyle = DisplayStyle.NATBIB;
        } else if (nameAsIsProperty.getValue()) {
            displayStyle = DisplayStyle.AS_IS;
        } else if (nameFirstLastProperty.getValue()) {
            displayStyle = DisplayStyle.FIRSTNAME_LASTNAME;
        }

        AbbreviationStyle abbreviationStyle = AbbreviationStyle.NONE;

        if (abbreviationEnabledProperty.getValue()) {
            abbreviationStyle = AbbreviationStyle.FULL;
        } else if (abbreviationLastNameOnlyProperty.getValue()) {
            abbreviationStyle = AbbreviationStyle.LASTNAME_ONLY;
        }

        preferences.storeMainTableNameFormatPreferences(new MainTableNameFormatPreferences(displayStyle, abbreviationStyle));
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public List<String> getRestartWarnings() {
        return new ArrayList<>();
    }

    public BooleanProperty namesNatbibProperty() {
        return namesNatbibProperty;
    }

    public BooleanProperty nameAsIsProperty() {
        return nameAsIsProperty;
    }

    public BooleanProperty nameFirstLastProperty() {
        return nameFirstLastProperty;
    }

    public BooleanProperty nameLastFirstProperty() {
        return nameLastFirstProperty;
    }

    public BooleanProperty abbreviationDisabledProperty() {
        return abbreviationDisabledProperty;
    }

    public BooleanProperty abbreviationEnabledProperty() {
        return abbreviationEnabledProperty;
    }

    public BooleanProperty abbreviationLastNameOnlyProperty() {
        return abbreviationLastNameOnlyProperty;
    }
}
