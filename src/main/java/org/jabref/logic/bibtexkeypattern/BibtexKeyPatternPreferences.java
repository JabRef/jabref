package org.jabref.logic.bibtexkeypattern;

import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;

public class BibtexKeyPatternPreferences {

    public enum KeyLetters {
        ALWAYS,         // CiteKeyA, CiteKeyB, CiteKeyC ...
        SECOND_WITH_A,  // CiteKey, CiteKeyA, CiteKeyB ...
        SECOND_WITH_B   // CiteKey, CiteKeyB, CiteKeyC ...
    }

    private final String keyPatternRegex;
    private final String keyPatternReplacement;
    private final KeyLetters keyLetters;
    private final GlobalBibtexKeyPattern keyPattern;
    private final Character keywordDelimiter;
    private final boolean avoidOverwritingCiteKey;
    private final boolean warningBeforeOverwritingCiteKey;
    private final boolean generateKeysBeforeSaving;
    private final String unwantedCharacters;

    public BibtexKeyPatternPreferences(String keyPatternRegex,
                                       String keyPatternReplacement,
                                       KeyLetters keyLetters,
                                       GlobalBibtexKeyPattern keyPattern,
                                       Character keywordDelimiter,
                                       boolean avoidOverwritingCiteKey,
                                       boolean warningBeforeOverwritingCiteKey,
                                       boolean generateKeysBeforeSaving,
                                       String unwantedCharacters) {

        this.keyPatternRegex = keyPatternRegex;
        this.keyPatternReplacement = keyPatternReplacement;
        this.keyLetters = keyLetters;
        this.keyPattern = keyPattern;
        this.keywordDelimiter = keywordDelimiter;
        this.avoidOverwritingCiteKey = avoidOverwritingCiteKey;
        this.warningBeforeOverwritingCiteKey = warningBeforeOverwritingCiteKey;
        this.generateKeysBeforeSaving = generateKeysBeforeSaving;
        this.unwantedCharacters = unwantedCharacters;
    }

    public String getKeyPatternRegex() {
        return keyPatternRegex;
    }

    public String getKeyPatternReplacement() {
        return keyPatternReplacement;
    }

    public KeyLetters getKeyLetters() {
        return keyLetters;
    }

    public GlobalBibtexKeyPattern getKeyPattern() {
        return keyPattern;
    }

    public Character getKeywordDelimiter() {
        return keywordDelimiter;
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

    public String getUnwantedCharacters() {
        return unwantedCharacters;
    }
}
