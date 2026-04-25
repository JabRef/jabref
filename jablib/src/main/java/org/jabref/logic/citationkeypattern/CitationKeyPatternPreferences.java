package org.jabref.logic.citationkeypattern;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class CitationKeyPatternPreferences {

    public enum KeySuffix {
        ALWAYS,         // CiteKeyA, CiteKeyB, CiteKeyC ...
        SECOND_WITH_A,  // CiteKey, CiteKeyA, CiteKeyB ...
        SECOND_WITH_B   // CiteKey, CiteKeyB, CiteKeyC ...
    }

    private static final GlobalCitationKeyPatterns DEFAULT_CITATION_KEY_PATTERN = GlobalCitationKeyPatterns.fromPattern("[auth][year]");
    private static final SimpleObjectProperty<Character> DEFAULT_KEYWORD_DELIMITER = new SimpleObjectProperty<>(',');

    private final BooleanProperty shouldTransliterateFieldsForCitationKey;
    private final BooleanProperty shouldAvoidOverwriteCiteKey;
    private final BooleanProperty shouldWarnBeforeOverwriteCiteKey;
    private final BooleanProperty shouldGenerateCiteKeysBeforeSaving;
    private final ObjectProperty<KeySuffix> keySuffix;
    private final StringProperty keyPatternRegex;
    private final StringProperty keyPatternReplacement;
    private final StringProperty unwantedCharacters;
    private final ObjectProperty<GlobalCitationKeyPatterns> keyPatterns;
    private final SimpleObjectProperty<Character> keywordDelimiter;

    /// @param keywordDelimiter should always be BibEntryProperties#keyWordDelimiterProperty
    public CitationKeyPatternPreferences(boolean shouldTransliterateFieldsForCitationKey,
                                         boolean shouldAvoidOverwriteCiteKey,
                                         boolean shouldWarnBeforeOverwriteCiteKey,
                                         boolean shouldGenerateCiteKeysBeforeSaving,
                                         KeySuffix keySuffix,
                                         String keyPatternRegex,
                                         String keyPatternReplacement,
                                         String unwantedCharacters,
                                         GlobalCitationKeyPatterns keyPatterns,
                                         ReadOnlyObjectProperty<Character> keywordDelimiter) {

        this.shouldTransliterateFieldsForCitationKey = new SimpleBooleanProperty(shouldTransliterateFieldsForCitationKey);
        this.shouldAvoidOverwriteCiteKey = new SimpleBooleanProperty(shouldAvoidOverwriteCiteKey);
        this.shouldWarnBeforeOverwriteCiteKey = new SimpleBooleanProperty(shouldWarnBeforeOverwriteCiteKey);
        this.shouldGenerateCiteKeysBeforeSaving = new SimpleBooleanProperty(shouldGenerateCiteKeysBeforeSaving);
        this.keySuffix = new SimpleObjectProperty<>(keySuffix);
        this.keyPatternRegex = new SimpleStringProperty(keyPatternRegex);
        this.keyPatternReplacement = new SimpleStringProperty(keyPatternReplacement);
        this.unwantedCharacters = new SimpleStringProperty(unwantedCharacters);
        this.keyPatterns = new SimpleObjectProperty<>(keyPatterns);

        this.keywordDelimiter = new SimpleObjectProperty<>();
        this.keywordDelimiter.bind(keywordDelimiter);
    }

    private CitationKeyPatternPreferences() {
        this(
                false,                        // shouldTransliterateFieldsForCitationKey
                false,                        // shouldAvoidOverwriteCiteKey
                true,                         // shouldWarnBeforeOverwriteCiteKey
                false,                        // shouldGenerateCiteKeysBeforeSaving
                KeySuffix.SECOND_WITH_A,
                "",                           // keyPatternRegex
                "",                           // keyPatternReplacement
                "-`ʹ:!;?^$",                  // unwantedCharacters
                DEFAULT_CITATION_KEY_PATTERN,
                new SimpleObjectProperty<>()  // keywordDelimiter
        );

        this.keywordDelimiter.bind(DEFAULT_KEYWORD_DELIMITER);
    }

    public static CitationKeyPatternPreferences getDefault() {
        return new CitationKeyPatternPreferences();
    }

    public void setAll(CitationKeyPatternPreferences preferences) {
        this.shouldTransliterateFieldsForCitationKey.set(preferences.shouldTransliterateFieldsForCitationKey.get());
        this.shouldAvoidOverwriteCiteKey.set(preferences.shouldAvoidOverwriteCiteKey.get());
        this.shouldWarnBeforeOverwriteCiteKey.set(preferences.shouldWarnBeforeOverwriteCiteKey.get());
        this.shouldGenerateCiteKeysBeforeSaving.set(preferences.shouldGenerateCiteKeysBeforeSaving.get());
        this.keySuffix.set(preferences.keySuffix.get());
        this.keyPatternRegex.set(preferences.keyPatternRegex.get());
        this.keyPatternReplacement.set(preferences.keyPatternReplacement.get());
        this.unwantedCharacters.set(preferences.unwantedCharacters.get());
        this.keyPatterns.set(preferences.keyPatterns.get());
        // keywordDelimiter is always bound to BibEntryPreferences#keywordDelimiter
    }

    public boolean shouldTransliterateFieldsForCitationKey() {
        return shouldTransliterateFieldsForCitationKey.get();
    }

    public BooleanProperty shouldTransliterateFieldsForCitationKeyProperty() {
        return shouldTransliterateFieldsForCitationKey;
    }

    public void setShouldTransliterateFieldsForCitationKey(boolean shouldTransliterateFieldsForCitationKey) {
        this.shouldTransliterateFieldsForCitationKey.set(shouldTransliterateFieldsForCitationKey);
    }

    public boolean shouldAvoidOverwriteCiteKey() {
        return shouldAvoidOverwriteCiteKey.get();
    }

    public BooleanProperty shouldAvoidOverwriteCiteKeyProperty() {
        return shouldAvoidOverwriteCiteKey;
    }

    public void setAvoidOverwriteCiteKey(boolean shouldAvoidOverwriteCiteKey) {
        this.shouldAvoidOverwriteCiteKey.set(shouldAvoidOverwriteCiteKey);
    }

    public boolean shouldWarnBeforeOverwriteCiteKey() {
        return shouldWarnBeforeOverwriteCiteKey.get();
    }

    public BooleanProperty shouldWarnBeforeOverwriteCiteKeyProperty() {
        return shouldWarnBeforeOverwriteCiteKey;
    }

    public void setWarnBeforeOverwriteCiteKey(boolean shouldWarnBeforeOverwriteCiteKey) {
        this.shouldWarnBeforeOverwriteCiteKey.set(shouldWarnBeforeOverwriteCiteKey);
    }

    public boolean shouldGenerateCiteKeysBeforeSaving() {
        return shouldGenerateCiteKeysBeforeSaving.get();
    }

    public BooleanProperty shouldGenerateCiteKeysBeforeSavingProperty() {
        return shouldGenerateCiteKeysBeforeSaving;
    }

    public void setGenerateCiteKeysBeforeSaving(boolean shouldGenerateCiteKeysBeforeSaving) {
        this.shouldGenerateCiteKeysBeforeSaving.set(shouldGenerateCiteKeysBeforeSaving);
    }

    public KeySuffix getKeySuffix() {
        return keySuffix.get();
    }

    public ObjectProperty<KeySuffix> keySuffixProperty() {
        return keySuffix;
    }

    public void setKeySuffix(KeySuffix keySuffix) {
        this.keySuffix.set(keySuffix);
    }

    public String getKeyPatternRegex() {
        return keyPatternRegex.get();
    }

    public StringProperty keyPatternRegexProperty() {
        return keyPatternRegex;
    }

    public void setKeyPatternRegex(String keyPatternRegex) {
        this.keyPatternRegex.set(keyPatternRegex);
    }

    public String getKeyPatternReplacement() {
        return keyPatternReplacement.get();
    }

    public StringProperty keyPatternReplacementProperty() {
        return keyPatternReplacement;
    }

    public void setKeyPatternReplacement(String keyPatternReplacement) {
        this.keyPatternReplacement.set(keyPatternReplacement);
    }

    public String getUnwantedCharacters() {
        return unwantedCharacters.get();
    }

    public StringProperty unwantedCharactersProperty() {
        return unwantedCharacters;
    }

    public void setUnwantedCharacters(String unwantedCharacters) {
        this.unwantedCharacters.set(unwantedCharacters);
    }

    public GlobalCitationKeyPatterns getKeyPatterns() {
        return keyPatterns.get();
    }

    public ObjectProperty<GlobalCitationKeyPatterns> keyPatternsProperty() {
        return keyPatterns;
    }

    public void setKeyPatterns(GlobalCitationKeyPatterns keyPatterns) {
        this.keyPatterns.set(keyPatterns);
    }

    public Character getKeywordDelimiter() {
        return keywordDelimiter.get();
    }

    public CitationKeyPatternPreferences withKeywordDelimiter(ReadOnlyObjectProperty<Character> newDelimiter) {
        if (this.keywordDelimiter.isBound()) {
            this.keywordDelimiter.unbind();
        }

        if (newDelimiter == null) {
            this.keywordDelimiter.bind(DEFAULT_KEYWORD_DELIMITER);
        } else {
            this.keywordDelimiter.bind(newDelimiter);
        }

        return this;
    }
}
