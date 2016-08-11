package net.sf.jabref.logic.bibtexkeypattern;

import net.sf.jabref.preferences.JabRefPreferences;

public class BibtexKeyPatternPreferences {

    private final String defaultBibtexKeyPattern;
    private final String keyPatternRegex;
    private final String keyPatternReplacement;
    private final boolean alwaysAddLetter;
    private final boolean firstLetterA;
    private final boolean enforceLegalKey;

    public BibtexKeyPatternPreferences(String defaultBibtexKeyPattern, String keyPatternRegex, String keyPatternReplacement,
            boolean alwaysAddLetter, boolean firstLetterA, boolean enforceLegalKey) {
        this.defaultBibtexKeyPattern = defaultBibtexKeyPattern;
        this.keyPatternRegex = keyPatternRegex;
        this.keyPatternReplacement = keyPatternReplacement;
        this.alwaysAddLetter = alwaysAddLetter;
        this.firstLetterA = firstLetterA;
        this.enforceLegalKey = enforceLegalKey;
    }

    public static BibtexKeyPatternPreferences fromPreferences(JabRefPreferences jabRefPreferences) {
        return new BibtexKeyPatternPreferences(jabRefPreferences.get(JabRefPreferences.DEFAULT_BIBTEX_KEY_PATTERN),
                jabRefPreferences.get(JabRefPreferences.KEY_PATTERN_REGEX),
                jabRefPreferences.get(JabRefPreferences.KEY_PATTERN_REPLACEMENT),
                jabRefPreferences.getBoolean(JabRefPreferences.KEY_GEN_ALWAYS_ADD_LETTER),
                jabRefPreferences.getBoolean(JabRefPreferences.KEY_GEN_FIRST_LETTER_A),
                jabRefPreferences.getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY));
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

    public String getDefaultBibtexKeyPattern() { return defaultBibtexKeyPattern;}
}
