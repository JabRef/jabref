package org.jabref.model.bibtexkeypattern;

import java.util.List;

public class GlobalBibtexKeyPattern extends AbstractBibtexKeyPattern {

    private List<String> defaultBibtexKeyPattern;

    public GlobalBibtexKeyPattern(List<String> bibtexKeyPattern) {
        defaultBibtexKeyPattern = bibtexKeyPattern;
    }

    public static GlobalBibtexKeyPattern fromPattern(String pattern) {
        return new GlobalBibtexKeyPattern(split(pattern));
    }

    @Override
    public List<String> getLastLevelBibtexKeyPattern(String key) {
        return defaultBibtexKeyPattern;
    }
}
