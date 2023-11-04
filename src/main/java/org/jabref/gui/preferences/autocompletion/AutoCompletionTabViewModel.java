package org.jabref.gui.preferences.autocompletion;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.autocompleter.AutoCompleteFirstNameMode;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.FieldFactory;

public class AutoCompletionTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty enableAutoCompleteProperty = new SimpleBooleanProperty();
    private final StringProperty autoCompleteFieldsProperty = new SimpleStringProperty();
    private final BooleanProperty autoCompleteFirstLastProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoCompleteLastFirstProperty = new SimpleBooleanProperty();
    private final BooleanProperty autoCompleteBothProperty = new SimpleBooleanProperty();
    private final BooleanProperty firstNameModeAbbreviatedProperty = new SimpleBooleanProperty();
    private final BooleanProperty firstNameModeFullProperty = new SimpleBooleanProperty();
    private final BooleanProperty firstNameModeBothProperty = new SimpleBooleanProperty();

    private final AutoCompletePreferences autoCompletePreferences;

    private final List<String> restartWarnings = new ArrayList<>();

    public AutoCompletionTabViewModel(AutoCompletePreferences autoCompletePreferences) {
        this.autoCompletePreferences = autoCompletePreferences;
    }

    @Override
    public void setValues() {
        enableAutoCompleteProperty.setValue(autoCompletePreferences.shouldAutoComplete());
        autoCompleteFieldsProperty.setValue(autoCompletePreferences.getCompleteNamesAsString());

        if (autoCompletePreferences.getNameFormat() == AutoCompletePreferences.NameFormat.FIRST_LAST) {
            autoCompleteFirstLastProperty.setValue(true);
        } else if (autoCompletePreferences.getNameFormat() == AutoCompletePreferences.NameFormat.LAST_FIRST) {
            autoCompleteLastFirstProperty.setValue(true);
        } else {
            autoCompleteBothProperty.setValue(true);
        }

        switch (autoCompletePreferences.getFirstNameMode()) {
            case ONLY_ABBREVIATED -> firstNameModeAbbreviatedProperty.setValue(true);
            case ONLY_FULL -> firstNameModeFullProperty.setValue(true);
            default -> firstNameModeBothProperty.setValue(true);
        }
    }

    @Override
    public void storeSettings() {
        autoCompletePreferences.setAutoComplete(enableAutoCompleteProperty.getValue());

        if (autoCompleteBothProperty.getValue()) {
            autoCompletePreferences.setNameFormat(AutoCompletePreferences.NameFormat.BOTH);
        } else if (autoCompleteFirstLastProperty.getValue()) {
            autoCompletePreferences.setNameFormat(AutoCompletePreferences.NameFormat.FIRST_LAST);
        } else if (autoCompleteLastFirstProperty.getValue()) {
            autoCompletePreferences.setNameFormat(AutoCompletePreferences.NameFormat.LAST_FIRST);
        }

        if (firstNameModeBothProperty.getValue()) {
            autoCompletePreferences.setFirstNameMode(AutoCompleteFirstNameMode.BOTH);
        } else if (firstNameModeAbbreviatedProperty.getValue()) {
            autoCompletePreferences.setFirstNameMode(AutoCompleteFirstNameMode.ONLY_ABBREVIATED);
        } else if (firstNameModeFullProperty.getValue()) {
            autoCompletePreferences.setFirstNameMode(AutoCompleteFirstNameMode.ONLY_FULL);
        }

        if (autoCompletePreferences.shouldAutoComplete() != enableAutoCompleteProperty.getValue()) {
            if (enableAutoCompleteProperty.getValue()) {
                restartWarnings.add(Localization.lang("Auto complete enabled."));
            } else {
                restartWarnings.add(Localization.lang("Auto complete disabled."));
            }
        }

        autoCompletePreferences.getCompleteFields().clear();
        autoCompletePreferences.getCompleteFields().addAll(FieldFactory.parseFieldList(autoCompleteFieldsProperty.getValue()));
    }

    @Override
    public List<String> getRestartWarnings() {
        return restartWarnings;
    }

    public BooleanProperty enableAutoCompleteProperty() {
        return enableAutoCompleteProperty;
    }

    public StringProperty autoCompleteFieldsProperty() {
        return autoCompleteFieldsProperty;
    }

    public BooleanProperty autoCompleteFirstLastProperty() {
        return autoCompleteFirstLastProperty;
    }

    public BooleanProperty autoCompleteLastFirstProperty() {
        return autoCompleteLastFirstProperty;
    }

    public BooleanProperty autoCompleteBothProperty() {
        return autoCompleteBothProperty;
    }

    public BooleanProperty firstNameModeAbbreviatedProperty() {
        return firstNameModeAbbreviatedProperty;
    }

    public BooleanProperty firstNameModeFullProperty() {
        return firstNameModeFullProperty;
    }

    public BooleanProperty firstNameModeBothProperty() {
        return firstNameModeBothProperty;
    }
}
