package org.jabref.logic.citationkeypattern;

import java.util.List;

import org.jabref.model.entry.types.EntryType;

public class GlobalCitationKeyPattern extends AbstractCitationKeyPattern {

    public GlobalCitationKeyPattern(List<String> bibtexKeyPattern) {
        defaultPattern = bibtexKeyPattern;
    }

    public static GlobalCitationKeyPattern fromPattern(String pattern) {
        return new GlobalCitationKeyPattern(split(pattern));
    }

    @Override
    public List<String> getLastLevelCitationKeyPattern(EntryType entryType) {
        return defaultPattern;
    }
}
