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

import org.jabref.gui.commonfxcontrols.CitationKeyPatternsPanelItemModel;
import org.jabref.gui.commonfxcontrols.CitationKeyPatternsPanelViewModel;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.importer.ImporterPreferences;

public class CitationKeyPatternTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty overwriteAllowProperty = new SimpleBooleanProperty();
    private final BooleanProperty overwriteWarningProperty = new SimpleBooleanProperty();
    private final BooleanProperty generateOnSaveProperty = new SimpleBooleanProperty();
    private final BooleanProperty generateKeyOnImportProperty = new SimpleBooleanProperty();
    private final BooleanProperty letterStartAProperty = new SimpleBooleanProperty();
    private final BooleanProperty letterStartBProperty = new SimpleBooleanProperty();
    private final BooleanProperty letterAlwaysAddProperty = new SimpleBooleanProperty();
    private final StringProperty keyPatternRegexProperty = new SimpleStringProperty();
    private final StringProperty keyPatternReplacementProperty = new SimpleStringProperty();
    private final StringProperty unwantedCharactersProperty = new SimpleStringProperty();

    // The list and the default properties are being overwritten by the bound properties of the tableView, but to
    // prevent an NPE on storing the preferences before lazy-loading of the setValues, they need to be initialized.
    private final ListProperty<CitationKeyPatternsPanelItemModel> patternListProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<CitationKeyPatternsPanelItemModel> defaultKeyPatternProperty = new SimpleObjectProperty<>(
            new CitationKeyPatternsPanelItemModel(new CitationKeyPatternsPanelViewModel.DefaultEntryType(), ""));

    private final CitationKeyPatternPreferences keyPatternPreferences;

    /**
     * The preference for whether to use the key generator on import is different from how it is configured.
     * In the UI, there is no better place to put the option than the Citation Key Generator tab.
     * However, shifting the preference to {@link CitationKeyPatternPreferences} would break the abstraction or hierarchy.
     * Hence, we keep the preference in {@link ImporterPreferences}, but for the UI, we initialize it here.
     */
    private final ImporterPreferences importerPreferences;

    public CitationKeyPatternTabViewModel(CitationKeyPatternPreferences keyPatternPreferences, ImporterPreferences importerPreferences) {
        this.keyPatternPreferences = keyPatternPreferences;
        this.importerPreferences = importerPreferences;
    }

    @Override
    public void setValues() {
        overwriteAllowProperty.setValue(!keyPatternPreferences.shouldAvoidOverwriteCiteKey());
        overwriteWarningProperty.setValue(keyPatternPreferences.shouldWarnBeforeOverwriteCiteKey());
        generateOnSaveProperty.setValue(keyPatternPreferences.shouldGenerateCiteKeysBeforeSaving());
        generateKeyOnImportProperty.setValue(importerPreferences.shouldGenerateNewKeyOnImport());

        if (keyPatternPreferences.getKeySuffix()
                == CitationKeyPatternPreferences.KeySuffix.ALWAYS) {
            letterAlwaysAddProperty.setValue(true);
            letterStartAProperty.setValue(false);
            letterStartBProperty.setValue(false);
        } else if (keyPatternPreferences.getKeySuffix()
                == CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A) {
            letterAlwaysAddProperty.setValue(false);
            letterStartAProperty.setValue(true);
            letterStartBProperty.setValue(false);
        } else {
            letterAlwaysAddProperty.setValue(false);
            letterStartAProperty.setValue(false);
            letterStartBProperty.setValue(true);
        }

        keyPatternRegexProperty.setValue(keyPatternPreferences.getKeyPatternRegex());
        keyPatternReplacementProperty.setValue(keyPatternPreferences.getKeyPatternReplacement());
        unwantedCharactersProperty.setValue(keyPatternPreferences.getUnwantedCharacters());
    }

    @Override
    public void storeSettings() {
        GlobalCitationKeyPatterns newKeyPattern =
                new GlobalCitationKeyPatterns(keyPatternPreferences.getKeyPatterns().getDefaultValue());
        patternListProperty.forEach(item -> {
            String patternString = item.getPattern();
            if (!"default".equals(item.getEntryType().getName())) {
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

        keyPatternPreferences.setAvoidOverwriteCiteKey(!overwriteAllowProperty.getValue());
        keyPatternPreferences.setWarnBeforeOverwriteCiteKey(overwriteWarningProperty.getValue());
        keyPatternPreferences.setGenerateCiteKeysBeforeSaving(generateOnSaveProperty.getValue());
        importerPreferences.setGenerateNewKeyOnImport(generateKeyOnImportProperty.getValue());
        keyPatternPreferences.setKeySuffix(keySuffix);
        keyPatternPreferences.setKeyPatternRegex(keyPatternRegexProperty.getValue());
        keyPatternPreferences.setKeyPatternReplacement(keyPatternReplacementProperty.getValue());
        keyPatternPreferences.setUnwantedCharacters(unwantedCharactersProperty.getValue());
        keyPatternPreferences.setKeyPatterns(newKeyPattern);
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

    public BooleanProperty generateKeyOnImportProperty() {
        return generateKeyOnImportProperty;
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

    public ListProperty<CitationKeyPatternsPanelItemModel> patternListProperty() {
        return patternListProperty;
    }

    public ObjectProperty<CitationKeyPatternsPanelItemModel> defaultKeyPatternProperty() {
        return defaultKeyPatternProperty;
    }

    public StringProperty unwantedCharactersProperty() {
        return unwantedCharactersProperty;
    }
}
