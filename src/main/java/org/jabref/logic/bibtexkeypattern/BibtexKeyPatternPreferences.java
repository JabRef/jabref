package org.jabref.logic.bibtexkeypattern;

import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;

public class BibtexKeyPatternPreferences {

    private final String keyPatternRegex;
    private final String keyPatternReplacement;
    private final boolean alwaysAddLetter;
    private final boolean firstLetterA;
    private final GlobalBibtexKeyPattern keyPattern;
    private Character keywordDelimiter;
    private boolean avoidOverwritingCiteKey;
    private String unwantedCharacters;

    public BibtexKeyPatternPreferences(String keyPatternRegex,
                                       String keyPatternReplacement,
                                       boolean alwaysAddLetter,
                                       boolean firstLetterA,
                                       GlobalBibtexKeyPattern keyPattern,
                                       Character keywordDelimiter,
                                       boolean avoidOverwritingCiteKey,
                                       String unwantedCharacters) {

        this.keyPatternRegex = keyPatternRegex;
        this.keyPatternReplacement = keyPatternReplacement;
        this.alwaysAddLetter = alwaysAddLetter;
        this.firstLetterA = firstLetterA;
        this.keyPattern = keyPattern;
        this.keywordDelimiter = keywordDelimiter;
        this.avoidOverwritingCiteKey = avoidOverwritingCiteKey;
        this.unwantedCharacters = unwantedCharacters;
    }

    public String getKeyPatternRegex() {
        return keyPatternRegex;
    }

    public String getKeyPatternReplacement() {
        return keyPatternReplacement;
    }

    public boolean isAlwaysAddLetter() {
        return alwaysAddLetter;
    }

    public boolean isFirstLetterA() {
        return firstLetterA;
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

    public boolean avoidOverwritingCiteKey() {
        return avoidOverwritingCiteKey;
    }
}
