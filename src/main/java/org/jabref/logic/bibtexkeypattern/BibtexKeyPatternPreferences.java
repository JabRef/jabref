package org.jabref.logic.bibtexkeypattern;

import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;

public class BibtexKeyPatternPreferences {

    public enum KeyLetters {
        ALWAYS,         // CiteKeyA, CiteKeyB, CiteKeyC ...
        SECOND_WITH_A,  // CiteKey, CiteKeyA, CiteKeyB ...
        SECOND_WITH_B   // CiteKey, CiteKeyB, CiteKeyC ...
    }

    private final boolean avoidOverwritingCiteKey;
    private final boolean warningBeforeOverwritingCiteKey;
    private final boolean generateKeysBeforeSaving;
    private final KeyLetters keyLetters;
    private final String keyPatternRegex;
    private final String keyPatternReplacement;
    private final String unwantedCharacters;
    private final GlobalBibtexKeyPattern keyPattern;
    private final Character keywordDelimiter;

    public BibtexKeyPatternPreferences(boolean avoidOverwritingCiteKey,
                                       boolean warningBeforeOverwritingCiteKey,
                                       boolean generateKeysBeforeSaving,
                                       KeyLetters keyLetters,
                                       String keyPatternRegex,
                                       String keyPatternReplacement,
                                       String unwantedCharacters,
                                       GlobalBibtexKeyPattern keyPattern,
                                       Character keywordDelimiter) {

        this.avoidOverwritingCiteKey = avoidOverwritingCiteKey;
        this.warningBeforeOverwritingCiteKey = warningBeforeOverwritingCiteKey;
        this.generateKeysBeforeSaving = generateKeysBeforeSaving;
        this.keyLetters = keyLetters;
        this.keyPatternRegex = keyPatternRegex;
        this.keyPatternReplacement = keyPatternReplacement;
        this.unwantedCharacters = unwantedCharacters;
        this.keyPattern = keyPattern;
        this.keywordDelimiter = keywordDelimiter;
    }

    public boolean avoidOverwritingCiteKey() {
        return avoidOverwritingCiteKey;
    }

    public boolean isWarningBeforeOverwrite() {
        return warningBeforeOverwritingCiteKey;
    }

    public boolean isGenerateKeysBeforeSaving() {
        return generateKeysBeforeSaving;
    }

    public KeyLetters getKeyLetters() {
        return keyLetters;
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
