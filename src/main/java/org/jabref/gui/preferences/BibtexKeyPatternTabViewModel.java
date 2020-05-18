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

import org.jabref.gui.DialogService;
import org.jabref.gui.commonfxcontrols.BibtexKeyPatternPanelItemModel;
import org.jabref.gui.commonfxcontrols.BibtexKeyPatternPanelViewModel;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.preferences.JabRefPreferences;

public class BibtexKeyPatternTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty overwriteAllowProperty = new SimpleBooleanProperty();
    private final BooleanProperty overwriteWarningProperty = new SimpleBooleanProperty();
    private final BooleanProperty generateOnSaveProperty = new SimpleBooleanProperty();
    private final BooleanProperty letterStartAProperty = new SimpleBooleanProperty();
    private final BooleanProperty letterStartBProperty = new SimpleBooleanProperty();
    private final BooleanProperty letterAlwaysAddProperty = new SimpleBooleanProperty();
    private final StringProperty keyPatternRegexProperty = new SimpleStringProperty();
    private final StringProperty keyPatternReplacementProperty = new SimpleStringProperty();
    private final StringProperty unwantedCharactersProperty = new SimpleStringProperty();

    // The list and the default properties are being overwritten by the bound properties of the tableView, but to
    // prevent an NPE on storing the preferences before lazy-loading of the setValues, they need to be initialized.
    private final ListProperty<BibtexKeyPatternPanelItemModel> patternListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<BibtexKeyPatternPanelItemModel> defaultKeyPatternProperty = new SimpleObjectProperty<>(
            new BibtexKeyPatternPanelItemModel(new BibtexKeyPatternPanelViewModel.DefaultEntryType(), ""));

    private final DialogService dialogService;
    private final JabRefPreferences preferences;
    private final BibtexKeyPatternPreferences initialBibtexKeyPatternPreferences;

    public BibtexKeyPatternTabViewModel(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.initialBibtexKeyPatternPreferences = preferences.getBibtexKeyPatternPreferences();
    }

    @Override
    public void setValues() {
        overwriteAllowProperty.setValue(!initialBibtexKeyPatternPreferences.avoidOverwritingCiteKey());
        overwriteWarningProperty.setValue(initialBibtexKeyPatternPreferences.isWarningBeforeOverwrite());
        generateOnSaveProperty.setValue(initialBibtexKeyPatternPreferences.isGenerateKeysBeforeSaving());

        if (initialBibtexKeyPatternPreferences.getKeyLetters()
                == BibtexKeyPatternPreferences.KeyLetters.ALWAYS) {
            letterAlwaysAddProperty.setValue(true);
            letterStartAProperty.setValue(false);
            letterStartBProperty.setValue(false);
        } else if (initialBibtexKeyPatternPreferences.getKeyLetters()
                == BibtexKeyPatternPreferences.KeyLetters.SECOND_WITH_A) {
            letterAlwaysAddProperty.setValue(false);
            letterStartAProperty.setValue(true);
            letterStartBProperty.setValue(false);
        } else {
            letterAlwaysAddProperty.setValue(false);
            letterStartAProperty.setValue(false);
            letterStartBProperty.setValue(true);
        }

        keyPatternRegexProperty.setValue(initialBibtexKeyPatternPreferences.getKeyPatternRegex());
        keyPatternReplacementProperty.setValue(initialBibtexKeyPatternPreferences.getKeyPatternReplacement());
        unwantedCharactersProperty.setValue(initialBibtexKeyPatternPreferences.getUnwantedCharacters());
    }

    @Override
    public void storeSettings() {
        GlobalBibtexKeyPattern newKeyPattern = GlobalBibtexKeyPattern.fromPattern(preferences.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN));
        patternListProperty.forEach(item -> {
            String patternString = item.getPattern();
            if (!item.getEntryType().getName().equals("default")) {
                if (!patternString.trim().isEmpty()) {
                    newKeyPattern.addBibtexKeyPattern(item.getEntryType(), patternString);
                }
            }
        });

        if (!defaultKeyPatternProperty.getValue().getPattern().trim().isEmpty()) {
            // we do not trim the value at the assignment to enable users to have spaces at the beginning and
            // at the end of the pattern
            newKeyPattern.setDefaultValue(defaultKeyPatternProperty.getValue().getPattern());
        }

        BibtexKeyPatternPreferences.KeyLetters keyLetters = BibtexKeyPatternPreferences.KeyLetters.ALWAYS;

        if (letterStartAProperty.getValue()) {
            keyLetters = BibtexKeyPatternPreferences.KeyLetters.SECOND_WITH_A;
        } else if (letterStartBProperty.getValue()) {
            keyLetters = BibtexKeyPatternPreferences.KeyLetters.SECOND_WITH_B;
        }

        preferences.storeBibtexKeyPatternPreferences(new BibtexKeyPatternPreferences(
                !overwriteAllowProperty.getValue(),
                overwriteWarningProperty.getValue(),
                generateOnSaveProperty.getValue(),
                keyLetters,
                keyPatternRegexProperty.getValue(),
                keyPatternReplacementProperty.getValue(),
                unwantedCharactersProperty.getValue(),
                newKeyPattern,
                initialBibtexKeyPatternPreferences.getKeywordDelimiter()));
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public List<String> getRestartWarnings() {
        return new ArrayList<>();
    }

    public BooleanProperty overwriteAllowProperty() {
        return overwriteAllowProperty;
    }

    public BooleanProperty overwriteWarningProperty() {
        return overwriteWarningProperty;
    }

    public BooleanProperty generateOnSaveProperty() {
        return generateOnSaveProperty;
    }

    public BooleanProperty letterStartAProperty() {
        return letterStartAProperty;
    }

    public BooleanProperty letterStartBProperty() {
        return letterStartBProperty;
    }

    public BooleanProperty letterAlwaysAddProperty() {
        return letterAlwaysAddProperty;
    }

    public StringProperty keyPatternRegexProperty() {
        return keyPatternRegexProperty;
    }

    public StringProperty keyPatternReplacementProperty() {
        return keyPatternReplacementProperty;
    }

    public ListProperty<BibtexKeyPatternPanelItemModel> patternListProperty() {
        return patternListProperty;
    }

    public ObjectProperty<BibtexKeyPatternPanelItemModel> defaultKeyPatternProperty() {
        return defaultKeyPatternProperty;
    }

    public StringProperty unwantedCharactersProperty() {
        return unwantedCharactersProperty;
    }
}
