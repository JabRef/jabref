package org.jabref.logic.citationkeypattern;

import java.util.List;

import org.jabref.model.entry.types.EntryType;

public class GlobalCitationKeyPatterns extends AbstractCitationKeyPatterns {

    public GlobalCitationKeyPatterns(List<String> bibtexKeyPattern) {
        defaultPattern = bibtexKeyPattern;
    }

    public static GlobalCitationKeyPatterns fromPattern(String pattern) {
        return new GlobalCitationKeyPatterns(split(pattern));
    }

    @Override
    public List<String> getLastLevelCitationKeyPattern(EntryType entryType) {
        return defaultPattern;
    }
}
