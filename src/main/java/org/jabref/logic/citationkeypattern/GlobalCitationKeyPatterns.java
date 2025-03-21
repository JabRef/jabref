package org.jabref.logic.citationkeypattern;

import org.jabref.model.entry.types.EntryType;

public class GlobalCitationKeyPatterns extends AbstractCitationKeyPatterns {

    public GlobalCitationKeyPatterns(Pattern defaultPattern) {
        this.defaultPattern = defaultPattern;
    }

    public static GlobalCitationKeyPatterns fromPattern(String pattern) {
        return new GlobalCitationKeyPatterns(new Pattern(pattern));
    }

    @Override
    public Pattern getLastLevelCitationKeyPattern(EntryType entryType) {
        return defaultPattern;
    }
}
