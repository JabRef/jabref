package org.jabref.gui.preferences.citationkeypattern;

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
import org.jabref.gui.commonfxcontrols.CitationKeyPatternPanelItemModel;
import org.jabref.gui.commonfxcontrols.CitationKeyPatternPanelViewModel;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.preferences.PreferencesService;

public class CitationKeyPatternTabViewModel implements PreferenceTabViewModel {

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
    private final ListProperty<CitationKeyPatternPanelItemModel> patternListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<CitationKeyPatternPanelItemModel> defaultKeyPatternProperty = new SimpleObjectProperty<>(
            new CitationKeyPatternPanelItemModel(new CitationKeyPatternPanelViewModel.DefaultEntryType(), ""));

    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final CitationKeyPatternPreferences initialCitationKeyPatternPreferences;

    public CitationKeyPatternTabViewModel(DialogService dialogService, PreferencesService preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.initialCitationKeyPatternPreferences = preferences.getCitationKeyPatternPreferences();
    }

    @Override
    public void setValues() {
        overwriteAllowProperty.setValue(!initialCitationKeyPatternPreferences.shouldAvoidOverwriteCiteKey());
        overwriteWarningProperty.setValue(initialCitationKeyPatternPreferences.shouldWarnBeforeOverwriteCiteKey());
        generateOnSaveProperty.setValue(initialCitationKeyPatternPreferences.shouldGenerateCiteKeysBeforeSaving());

        if (initialCitationKeyPatternPreferences.getKeySuffix()
                == CitationKeyPatternPreferences.KeySuffix.ALWAYS) {
            letterAlwaysAddProperty.setValue(true);
            letterStartAProperty.setValue(false);
            letterStartBProperty.setValue(false);
        } else if (initialCitationKeyPatternPreferences.getKeySuffix()
                == CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A) {
            letterAlwaysAddProperty.setValue(false);
            letterStartAProperty.setValue(true);
            letterStartBProperty.setValue(false);
        } else {
            letterAlwaysAddProperty.setValue(false);
            letterStartAProperty.setValue(false);
            letterStartBProperty.setValue(true);
        }

        keyPatternRegexProperty.setValue(initialCitationKeyPatternPreferences.getKeyPatternRegex());
        keyPatternReplacementProperty.setValue(initialCitationKeyPatternPreferences.getKeyPatternReplacement());
        unwantedCharactersProperty.setValue(initialCitationKeyPatternPreferences.getUnwantedCharacters());
    }

    @Override
    public void storeSettings() {
        GlobalCitationKeyPattern newKeyPattern =
                new GlobalCitationKeyPattern(initialCitationKeyPatternPreferences.getKeyPattern().getDefaultValue());
        patternListProperty.forEach(item -> {
            String patternString = item.getPattern();
            if (!item.getEntryType().getName().equals("default")) {
                if (!patternString.trim().isEmpty()) {
                    newKeyPattern.addCitationKeyPattern(item.getEntryType(), patternString);
                }
            }
        });

        if (!defaultKeyPatternProperty.getValue().getPattern().trim().isEmpty()) {
            // we do not trim the value at the assignment to enable users to have spaces at the beginning and
            // at the end of the pattern
            newKeyPattern.setDefaultValue(defaultKeyPatternProperty.getValue().getPattern());
        }

        CitationKeyPatternPreferences.KeySuffix keySuffix = CitationKeyPatternPreferences.KeySuffix.ALWAYS;

        if (letterStartAProperty.getValue()) {
            keySuffix = CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A;
        } else if (letterStartBProperty.getValue()) {
            keySuffix = CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_B;
        }

        preferences.storeCitationKeyPatternPreferences(new CitationKeyPatternPreferences(
                !overwriteAllowProperty.getValue(),
                overwriteWarningProperty.getValue(),
                generateOnSaveProperty.getValue(),
                keySuffix,
                keyPatternRegexProperty.getValue(),
                keyPatternReplacementProperty.getValue(),
                unwantedCharactersProperty.getValue(),
                newKeyPattern,
                initialCitationKeyPatternPreferences.getKeywordDelimiter()));
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

    public ListProperty<CitationKeyPatternPanelItemModel> patternListProperty() {
        return patternListProperty;
    }

    public ObjectProperty<CitationKeyPatternPanelItemModel> defaultKeyPatternProperty() {
        return defaultKeyPatternProperty;
    }

    public StringProperty unwantedCharactersProperty() {
        return unwantedCharactersProperty;
    }
}
