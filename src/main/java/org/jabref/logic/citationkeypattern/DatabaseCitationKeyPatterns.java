package org.jabref.logic.citationkeypattern;

import java.util.List;

import org.jabref.model.entry.types.EntryType;

public class DatabaseCitationKeyPatterns extends AbstractCitationKeyPatterns {

    private final GlobalCitationKeyPatterns globalCitationKeyPattern;

    public DatabaseCitationKeyPatterns(GlobalCitationKeyPatterns globalCitationKeyPattern) {
        this.globalCitationKeyPattern = globalCitationKeyPattern;
    }

    @Override
    public List<String> getLastLevelCitationKeyPattern(EntryType entryType) {
        return globalCitationKeyPattern.getValue(entryType);
    }
}
