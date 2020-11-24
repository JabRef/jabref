package org.jabref.logic.citationkeypattern;

import java.util.List;

import org.jabref.model.entry.types.EntryType;

public class DatabaseCitationKeyPattern extends AbstractCitationKeyPattern {

    private final GlobalCitationKeyPattern globalCitationKeyPattern;

    public DatabaseCitationKeyPattern(GlobalCitationKeyPattern globalCitationKeyPattern) {
        this.globalCitationKeyPattern = globalCitationKeyPattern;
    }

    @Override
    public List<String> getLastLevelCitationKeyPattern(EntryType entryType) {
        return globalCitationKeyPattern.getValue(entryType);
    }
}
