package org.jabref.logic.citationkeypattern;

import org.jabref.model.entry.types.EntryType;

public class GlobalCitationKeyPatterns extends AbstractCitationKeyPatterns {

    public GlobalCitationKeyPatterns(CitationKeyPattern defaultPattern) {
        this.defaultPattern = defaultPattern;
    }

    public static GlobalCitationKeyPatterns fromPattern(String pattern) {
        return new GlobalCitationKeyPatterns(new CitationKeyPattern(pattern));
    }

    @Override
    public CitationKeyPattern getLastLevelCitationKeyPattern(EntryType entryType) {
        return defaultPattern;
    }
}
