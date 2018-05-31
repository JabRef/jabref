package org.jabref.logic.util.io;

public class AutoLinkPreferences {
    boolean useRegularExpression;
    String regularExpression;
    Character keywordDelimiter;
    boolean onlyFindByExactCiteKey;

    public AutoLinkPreferences(boolean useRegularExpression, String regularExpression, boolean onlyFindByExactCiteKey, Character keywordDelimiter) {
        this.useRegularExpression = useRegularExpression;
        this.regularExpression = regularExpression;
        this.onlyFindByExactCiteKey = onlyFindByExactCiteKey;
        this.keywordDelimiter = keywordDelimiter;
    }

    public boolean isUseRegularExpression() {
        return useRegularExpression;
    }

    public String getRegularExpression() {
        return regularExpression;
    }

    public boolean isOnlyFindByExactCiteKey() {
        return onlyFindByExactCiteKey;
    }

    public Character getKeywordDelimiter() {
        return keywordDelimiter;
    }
}
