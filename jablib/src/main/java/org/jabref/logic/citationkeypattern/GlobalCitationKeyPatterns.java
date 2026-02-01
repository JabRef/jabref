package org.jabref.logic.citationkeypattern;

import java.util.HashMap;

import org.jabref.model.entry.types.EntryType;

public class GlobalCitationKeyPatterns extends AbstractCitationKeyPatterns {

    public GlobalCitationKeyPatterns(CitationKeyPattern defaultPattern) {
        this.defaultPattern = defaultPattern;
    }

    public static GlobalCitationKeyPatterns fromPattern(String pattern) {
        return new GlobalCitationKeyPatterns(new CitationKeyPattern(pattern));
    }

    public GlobalCitationKeyPatterns copy() {
        GlobalCitationKeyPatterns copy = new GlobalCitationKeyPatterns(this.defaultPattern);
        copy.data = new HashMap<>(this.data);
        return copy;
    }

    public void setDefaultPattern(String pattern) {
        this.defaultPattern = new CitationKeyPattern(pattern);
    }

    @Override
    public CitationKeyPattern getLastLevelCitationKeyPattern(EntryType entryType) {
        return defaultPattern;
    }
}
