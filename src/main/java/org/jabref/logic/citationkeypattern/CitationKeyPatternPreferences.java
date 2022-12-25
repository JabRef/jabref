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

    private final BooleanProperty shouldAvoidOverwriteCiteKey = new SimpleBooleanProperty();
    private final BooleanProperty shouldWarnBeforeOverwriteCiteKey = new SimpleBooleanProperty();
    private final BooleanProperty shouldGenerateCiteKeysBeforeSaving = new SimpleBooleanProperty();
    private final ObjectProperty<KeySuffix> keySuffix = new SimpleObjectProperty<>();
    private final StringProperty keyPatternRegex = new SimpleStringProperty();
    private final StringProperty keyPatternReplacement = new SimpleStringProperty();
    private final StringProperty unwantedCharacters = new SimpleStringProperty();
    private final ObjectProperty<GlobalCitationKeyPattern> keyPattern = new SimpleObjectProperty<>();
    private final String defaultPattern;
    private final ReadOnlyObjectProperty<Character> keywordDelimiter;

    public CitationKeyPatternPreferences(boolean shouldAvoidOverwriteCiteKey,
                                         boolean shouldWarnBeforeOverwriteCiteKey,
                                         boolean shouldGenerateCiteKeysBeforeSaving,
                                         KeySuffix keySuffix,
                                         String keyPatternRegex,
                                         String keyPatternReplacement,
                                         String unwantedCharacters,
                                         GlobalCitationKeyPattern keyPattern,
                                         String defaultPattern,
                                         ReadOnlyObjectProperty<Character> keywordDelimiter) {

        this.shouldAvoidOverwriteCiteKey.set(shouldAvoidOverwriteCiteKey);
        this.shouldWarnBeforeOverwriteCiteKey.set(shouldWarnBeforeOverwriteCiteKey);
        this.shouldGenerateCiteKeysBeforeSaving.set(shouldGenerateCiteKeysBeforeSaving);
        this.keySuffix.set(keySuffix);
        this.keyPatternRegex.set(keyPatternRegex);
        this.keyPatternReplacement.set(keyPatternReplacement);
        this.unwantedCharacters.set(unwantedCharacters);
        this.keyPattern.set(keyPattern);

        this.defaultPattern = defaultPattern;
        this.keywordDelimiter = keywordDelimiter;
    }

    /**
     * For use in test
     */
    public CitationKeyPatternPreferences(boolean shouldAvoidOverwriteCiteKey,
                                         boolean shouldWarnBeforeOverwriteCiteKey,
                                         boolean shouldGenerateCiteKeysBeforeSaving,
                                         KeySuffix keySuffix,
                                         String keyPatternRegex,
                                         String keyPatternReplacement,
                                         String unwantedCharacters,
                                         GlobalCitationKeyPattern keyPattern,
                                         String defaultPattern,
                                         Character keywordDelimiter) {

        this(shouldAvoidOverwriteCiteKey,
                shouldWarnBeforeOverwriteCiteKey,
                shouldGenerateCiteKeysBeforeSaving,
                keySuffix,
                keyPatternRegex,
                keyPatternReplacement,
                unwantedCharacters,
                keyPattern,
                defaultPattern,
                new SimpleObjectProperty<>(keywordDelimiter));
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

    public GlobalCitationKeyPattern getKeyPattern() {
        return keyPattern.get();
    }

    public ObjectProperty<GlobalCitationKeyPattern> keyPatternProperty() {
        return keyPattern;
    }

    public void setKeyPattern(GlobalCitationKeyPattern keyPattern) {
        this.keyPattern.set(keyPattern);
    }

    public String getDefaultPattern() {
        return defaultPattern;
    }

    public Character getKeywordDelimiter() {
        return keywordDelimiter.get();
    }
}
