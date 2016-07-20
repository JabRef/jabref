package net.sf.jabref.logic.labelpattern;

import net.sf.jabref.preferences.JabRefPreferences;

public class LabelPatternPreferences {

    private final String defaultLabelPattern;
    private final String keyPatternRegex;
    private final String keyPatternReplacement;
    private final boolean alwaysAddLetter;
    private final boolean firstLetterA;

    public LabelPatternPreferences(String defaultLabelPattern, String keyPatternRegex, String keyPatternReplacement,
            boolean alwaysAddLetter, boolean firstLetterA) {
        this.defaultLabelPattern = defaultLabelPattern;
        this.keyPatternRegex = keyPatternRegex;
        this.keyPatternReplacement = keyPatternReplacement;
        this.alwaysAddLetter = alwaysAddLetter;
        this.firstLetterA = firstLetterA;
    }

    public static LabelPatternPreferences fromPreferences(JabRefPreferences jabRefPreferences) {
        return new LabelPatternPreferences(jabRefPreferences.get(JabRefPreferences.DEFAULT_LABEL_PATTERN),
                jabRefPreferences.get(JabRefPreferences.KEY_PATTERN_REGEX),
                jabRefPreferences.get(JabRefPreferences.KEY_PATTERN_REPLACEMENT),
                jabRefPreferences.getBoolean(JabRefPreferences.KEY_GEN_ALWAYS_ADD_LETTER),
                jabRefPreferences.getBoolean(JabRefPreferences.KEY_GEN_FIRST_LETTER_A));
    }

    public String getDefaultLabelPattern() {
        return defaultLabelPattern;
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
}
