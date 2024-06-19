package org.jabref.logic.citationkeypattern;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;

public class GlobalCitationKeyPatterns extends AbstractCitationKeyPatterns {

    private final CitationKeyPattern defaultPattern;

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

    @Override
    public String getPattern(BibEntry entry) {
        EntryType entryType = entry.getType();
        CitationKeyPattern pattern = getValue(entryType);
        return pattern != null ? pattern.stringRepresentation() : defaultPattern.stringRepresentation();
    }
}
