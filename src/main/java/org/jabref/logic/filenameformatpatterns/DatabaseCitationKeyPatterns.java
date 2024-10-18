package org.jabref.logic.citationkeypattern;

import org.jabref.model.entry.types.EntryType;

public class DatabaseCitationKeyPatterns extends AbstractFilenameFormatPatterns {

    private final GlobalCitationKeyPatterns globalCitationKeyPattern;

    public DatabaseCitationKeyPatterns(GlobalCitationKeyPatterns globalCitationKeyPattern) {
        this.globalCitationKeyPattern = globalCitationKeyPattern;
    }

    @Override
    public FilenameFormatPattern getLastLevelCitationKeyPattern(EntryType entryType) {
        return globalCitationKeyPattern.getValue(entryType);
    }
}
