package org.jabref.logic.bibtexkeypattern;

import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;

public class BibtexKeyPatternPreferences {

    private final String keyPatternRegex;
    private final String keyPatternReplacement;
    private final boolean alwaysAddLetter;
    private final boolean firstLetterA;
    private final boolean enforceLegalKey;
    private final GlobalBibtexKeyPattern keyPattern;
    private Character keywordDelimiter;

    public BibtexKeyPatternPreferences(String keyPatternRegex, String keyPatternReplacement, boolean alwaysAddLetter,
            boolean firstLetterA, boolean enforceLegalKey, GlobalBibtexKeyPattern keyPattern,
            Character keywordDelimiter) {
        this.keyPatternRegex = keyPatternRegex;
        this.keyPatternReplacement = keyPatternReplacement;
        this.alwaysAddLetter = alwaysAddLetter;
        this.firstLetterA = firstLetterA;
        this.enforceLegalKey = enforceLegalKey;
        this.keyPattern = keyPattern;
        this.keywordDelimiter = keywordDelimiter;
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

    public boolean isEnforceLegalKey() {
        return enforceLegalKey;
    }

    public GlobalBibtexKeyPattern getKeyPattern() {
        return keyPattern;
    }

    public Character getKeywordDelimiter() {
        return keywordDelimiter;
    }
}
