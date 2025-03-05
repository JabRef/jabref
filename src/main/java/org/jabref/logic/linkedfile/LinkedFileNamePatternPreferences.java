package org.jabref.logic.linkedfile;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import com.google.common.annotations.VisibleForTesting;

public class LinkedFileNamePatternPreferences {

    private final BooleanProperty shouldAvoidOverwriteCiteKey = new SimpleBooleanProperty();
    private final BooleanProperty shouldWarnBeforeOverwriteCiteKey = new SimpleBooleanProperty();
    private final BooleanProperty shouldGenerateCiteKeysBeforeSaving = new SimpleBooleanProperty();
    private final ObjectProperty<org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences.KeySuffix> keySuffix = new SimpleObjectProperty<>();
    private final StringProperty keyPatternRegex = new SimpleStringProperty();
    private final StringProperty keyPatternReplacement = new SimpleStringProperty();
    private final StringProperty unwantedCharacters = new SimpleStringProperty();
    private final ObjectProperty<GlobalLinkedFileNamePatterns> keyPatterns = new SimpleObjectProperty<>();
    private final String defaultPattern;
    private final ReadOnlyObjectProperty<Character> keywordDelimiter;

    public LinkedFileNamePatternPreferences(boolean shouldAvoidOverwriteCiteKey,
                                         boolean shouldWarnBeforeOverwriteCiteKey,
                                         boolean shouldGenerateCiteKeysBeforeSaving,
                                         org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences.KeySuffix keySuffix,
                                         String keyPatternRegex,
                                         String keyPatternReplacement,
                                         String unwantedCharacters,
                                            GlobalLinkedFileNamePatterns keyPatterns,
                                         String defaultPattern,
                                         ReadOnlyObjectProperty<Character> keywordDelimiter) {

        this.shouldAvoidOverwriteCiteKey.set(shouldAvoidOverwriteCiteKey);
        this.shouldWarnBeforeOverwriteCiteKey.set(shouldWarnBeforeOverwriteCiteKey);
        this.shouldGenerateCiteKeysBeforeSaving.set(shouldGenerateCiteKeysBeforeSaving);
        this.keySuffix.set(keySuffix);
        this.keyPatternRegex.set(keyPatternRegex);
        this.keyPatternReplacement.set(keyPatternReplacement);
        this.unwantedCharacters.set(unwantedCharacters);
        this.keyPatterns.set(keyPatterns);

        this.defaultPattern = defaultPattern;
        this.keywordDelimiter = keywordDelimiter;
    }

    @VisibleForTesting
    public LinkedFileNamePatternPreferences(boolean shouldAvoidOverwriteCiteKey,
                                         boolean shouldWarnBeforeOverwriteCiteKey,
                                         boolean shouldGenerateCiteKeysBeforeSaving,
                                         org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences.KeySuffix keySuffix,
                                         String keyPatternRegex,
                                         String keyPatternReplacement,
                                         String unwantedCharacters,
                                            GlobalLinkedFileNamePatterns keyPatterns,
                                         String defaultPattern,
                                         Character keywordDelimiter) {

        this(shouldAvoidOverwriteCiteKey,
                shouldWarnBeforeOverwriteCiteKey,
                shouldGenerateCiteKeysBeforeSaving,
                keySuffix,
                keyPatternRegex,
                keyPatternReplacement,
                unwantedCharacters,
                keyPatterns,
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

    public org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences.KeySuffix getKeySuffix() {
        return keySuffix.get();
    }

    public ObjectProperty<org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences.KeySuffix> keySuffixProperty() {
        return keySuffix;
    }

    public void setKeySuffix(org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences.KeySuffix keySuffix) {
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

    public GlobalLinkedFileNamePatterns getKeyPatterns() {
        return keyPatterns.get();
    }

    public ObjectProperty<GlobalLinkedFileNamePatterns> keyPatternsProperty() {
        return keyPatterns;
    }

    public void setKeyPatterns(GlobalLinkedFileNamePatterns keyPatterns) {
        this.keyPatterns.set(keyPatterns);
    }

    public String getDefaultPattern() {
        return defaultPattern;
    }

    public Character getKeywordDelimiter() {
        return keywordDelimiter.get();
    }
}
