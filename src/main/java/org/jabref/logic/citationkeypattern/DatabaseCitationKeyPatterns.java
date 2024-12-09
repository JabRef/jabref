package org.jabref.logic.citationkeypattern;

import org.jabref.model.entry.types.EntryType;

public class DatabaseCitationKeyPatterns extends AbstractCitationKeyPatterns {

    private final GlobalCitationKeyPatterns globalCitationKeyPattern;

    public DatabaseCitationKeyPatterns(GlobalCitationKeyPatterns globalCitationKeyPattern) {
        this.globalCitationKeyPattern = globalCitationKeyPattern;
    }

    @Override
    public CitationKeyPattern getLastLevelCitationKeyPattern(EntryType entryType) {
        return globalCitationKeyPattern.getValue(entryType);
    }
}
