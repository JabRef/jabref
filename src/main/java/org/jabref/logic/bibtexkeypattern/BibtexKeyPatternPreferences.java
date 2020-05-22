package org.jabref.logic.bibtexkeypattern;

import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;

public class BibtexKeyPatternPreferences {

    public enum KeySuffix {
        ALWAYS,         // CiteKeyA, CiteKeyB, CiteKeyC ...
        SECOND_WITH_A,  // CiteKey, CiteKeyA, CiteKeyB ...
        SECOND_WITH_B   // CiteKey, CiteKeyB, CiteKeyC ...
    }

    private final boolean shouldAvoidOverwriteCiteKey;
    private final boolean shouldWarnBeforeOverwriteCiteKey;
    private final boolean shouldGenerateCiteKeysBeforeSaving;
    private final KeySuffix keySuffix;
    private final String keyPatternRegex;
    private final String keyPatternReplacement;
    private final String unwantedCharacters;
    private final GlobalBibtexKeyPattern keyPattern;
    private final Character keywordDelimiter;

    public BibtexKeyPatternPreferences(boolean shouldAvoidOverwriteCiteKey,
                                       boolean shouldWarnBeforeOverwriteCiteKey,
                                       boolean shouldGenerateCiteKeysBeforeSaving,
                                       KeySuffix keySuffix,
                                       String keyPatternRegex,
                                       String keyPatternReplacement,
                                       String unwantedCharacters,
                                       GlobalBibtexKeyPattern keyPattern,
                                       Character keywordDelimiter) {

        this.shouldAvoidOverwriteCiteKey = shouldAvoidOverwriteCiteKey;
        this.shouldWarnBeforeOverwriteCiteKey = shouldWarnBeforeOverwriteCiteKey;
        this.shouldGenerateCiteKeysBeforeSaving = shouldGenerateCiteKeysBeforeSaving;
        this.keySuffix = keySuffix;
        this.keyPatternRegex = keyPatternRegex;
        this.keyPatternReplacement = keyPatternReplacement;
        this.unwantedCharacters = unwantedCharacters;
        this.keyPattern = keyPattern;
        this.keywordDelimiter = keywordDelimiter;
    }

    public boolean shouldAvoidOverwriteCiteKey() {
        return shouldAvoidOverwriteCiteKey;
    }

    public boolean shouldWarnBeforeOverwriteCiteKey() {
        return shouldWarnBeforeOverwriteCiteKey;
    }

    public boolean shouldGenerateCiteKeysBeforeSaving() {
        return shouldGenerateCiteKeysBeforeSaving;
    }

    public KeySuffix getKeySuffix() {
        return keySuffix;
    }

    public String getKeyPatternRegex() {
        return keyPatternRegex;
    }

    public String getKeyPatternReplacement() {
        return keyPatternReplacement;
    }

    public String getUnwantedCharacters() {
        return unwantedCharacters;
    }

    public GlobalBibtexKeyPattern getKeyPattern() {
        return keyPattern;
    }

    public Character getKeywordDelimiter() {
        return keywordDelimiter;
    }
}
