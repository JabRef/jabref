package org.jabref.logic.citationkeypattern;

import org.jabref.model.entry.types.EntryType;

public class GlobalCitationKeyPatterns extends AbstractFilenameFormatPatterns {

    public GlobalCitationKeyPatterns(FilenameFormatPattern defaultPattern) {
        this.defaultPattern = defaultPattern;
    }

    public static GlobalCitationKeyPatterns fromPattern(String pattern) {
        return new GlobalCitationKeyPatterns(new FilenameFormatPattern(pattern));
    }

    @Override
    public FilenameFormatPattern getLastLevelCitationKeyPattern(EntryType entryType) {
        return defaultPattern;
    }
}
