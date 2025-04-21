package org.jabref.logic.citationkeypattern;

import org.jabref.model.entry.types.EntryType;

public class GlobalCitationKeyPatterns extends AbstractCitationKeyPatterns {

    public GlobalCitationKeyPatterns(KeyPattern defaultPattern) {
        this.defaultPattern = defaultPattern;
    }

    public static GlobalCitationKeyPatterns fromPattern(String pattern) {
        return new GlobalCitationKeyPatterns(new KeyPattern(pattern));
    }

    @Override
    public KeyPattern getLastLevelCitationKeyPattern(EntryType entryType) {
        return defaultPattern;
    }
}
