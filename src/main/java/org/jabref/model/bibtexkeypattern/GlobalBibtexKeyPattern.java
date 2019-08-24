package org.jabref.model.bibtexkeypattern;

import java.util.List;

import org.jabref.model.entry.types.EntryType;

public class GlobalBibtexKeyPattern extends AbstractBibtexKeyPattern {

    private List<String> defaultBibtexKeyPattern;

    public GlobalBibtexKeyPattern(List<String> bibtexKeyPattern) {
        defaultBibtexKeyPattern = bibtexKeyPattern;
    }

    public static GlobalBibtexKeyPattern fromPattern(String pattern) {
        return new GlobalBibtexKeyPattern(split(pattern));
    }

    @Override
    public List<String> getLastLevelBibtexKeyPattern(EntryType entryType) {
        return defaultBibtexKeyPattern;
    }
}
