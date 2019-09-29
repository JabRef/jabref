package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.preferences.JabRefPreferences;

public class BibtexKeyPatternTabViewModel implements PreferenceTabViewModel {

    private StringProperty defaultKeyProperty = new SimpleStringProperty();
    private BooleanProperty overwriteAllowProperty = new SimpleBooleanProperty();
    private BooleanProperty overwriteWarningProperty = new SimpleBooleanProperty();
    private BooleanProperty generateOnSaveProperty = new SimpleBooleanProperty();
    private BooleanProperty letterStartAProperty = new SimpleBooleanProperty();
    private BooleanProperty letterStartBProperty = new SimpleBooleanProperty();
    private BooleanProperty letterAlwaysAddProperty = new SimpleBooleanProperty();
    private StringProperty keyPatternRegexProperty = new SimpleStringProperty();
    private StringProperty keyPatternReplacementProperty = new SimpleStringProperty();

    private final DialogService dialogService;
    private final JabRefPreferences preferences;

    public BibtexKeyPatternTabViewModel(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
    }

    @Override
    public void setValues() {
        defaultKeyProperty.setValue(preferences.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN));
        overwriteAllowProperty.setValue(!preferences.getBoolean(JabRefPreferences.AVOID_OVERWRITING_KEY));
        overwriteWarningProperty.setValue(preferences.getBoolean(JabRefPreferences.WARN_BEFORE_OVERWRITING_KEY));
        generateOnSaveProperty.setValue(preferences.getBoolean(JabRefPreferences.GENERATE_KEYS_BEFORE_SAVING));

        if (preferences.getBoolean(JabRefPreferences.KEY_GEN_ALWAYS_ADD_LETTER)) {
            letterAlwaysAddProperty.setValue(true);
            letterStartAProperty.setValue(false);
            letterStartBProperty.setValue(false);
        } else if (preferences.getBoolean(JabRefPreferences.KEY_GEN_FIRST_LETTER_A)) {
            letterAlwaysAddProperty.setValue(false);
            letterStartAProperty.setValue(true);
            letterStartBProperty.setValue(false);
        } else {
            letterAlwaysAddProperty.setValue(false);
            letterStartAProperty.setValue(false);
            letterStartBProperty.setValue(true);
        }

        keyPatternRegexProperty.setValue(preferences.get(JabRefPreferences.KEY_PATTERN_REGEX));
        keyPatternReplacementProperty.setValue(preferences.get(JabRefPreferences.KEY_PATTERN_REPLACEMENT));
    }

    @Override
    public void storeSettings() {
        preferences.put(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN, defaultKeyProperty.getValue());
        preferences.putBoolean(JabRefPreferences.AVOID_OVERWRITING_KEY, !overwriteAllowProperty.getValue());
        preferences.putBoolean(JabRefPreferences.WARN_BEFORE_OVERWRITING_KEY, overwriteWarningProperty.getValue());
        preferences.putBoolean(JabRefPreferences.GENERATE_KEYS_BEFORE_SAVING, generateOnSaveProperty.getValue());

        if (letterAlwaysAddProperty.getValue()) {
            preferences.putBoolean(JabRefPreferences.KEY_GEN_ALWAYS_ADD_LETTER, true);
            preferences.putBoolean(JabRefPreferences.KEY_GEN_FIRST_LETTER_A, false);
        } else if (letterStartAProperty.getValue()) {
            preferences.putBoolean(JabRefPreferences.KEY_GEN_ALWAYS_ADD_LETTER, false);
            preferences.putBoolean(JabRefPreferences.KEY_GEN_FIRST_LETTER_A, true);
        } else if (letterStartBProperty.getValue()) {
            preferences.putBoolean(JabRefPreferences.KEY_GEN_ALWAYS_ADD_LETTER, false);
            preferences.putBoolean(JabRefPreferences.KEY_GEN_FIRST_LETTER_A, false);
        } else {
            // No Radioitem selected, should not happen, but if, make KEY_GEN_FIRST_LETTER_A default
            preferences.putBoolean(JabRefPreferences.KEY_GEN_ALWAYS_ADD_LETTER, false);
            preferences.putBoolean(JabRefPreferences.KEY_GEN_FIRST_LETTER_A, true);
        }

        preferences.put(JabRefPreferences.KEY_PATTERN_REGEX, keyPatternRegexProperty.getValue());
        preferences.put(JabRefPreferences.KEY_PATTERN_REPLACEMENT, keyPatternReplacementProperty.getValue());
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public List<String> getRestartWarnings() {
        return new ArrayList<>();
    }

    public StringProperty defaultKeyProperty() { return defaultKeyProperty; }

    public BooleanProperty overwriteAllowProperty() { return overwriteAllowProperty; }

    public BooleanProperty overwriteWarningProperty() { return overwriteWarningProperty; }

    public BooleanProperty generateOnSaveProperty() { return generateOnSaveProperty; }

    public BooleanProperty letterStartAProperty() { return letterStartAProperty; }

    public BooleanProperty letterStartBProperty() { return letterStartBProperty; }

    public BooleanProperty letterAlwaysAddProperty() { return letterAlwaysAddProperty; }

    public StringProperty keyPatternRegexProperty() { return keyPatternRegexProperty; }

    public StringProperty keyPatternReplacementProperty() { return keyPatternReplacementProperty; }
}
