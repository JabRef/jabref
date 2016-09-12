package net.sf.jabref.model.bibtexkeypattern;

import java.util.List;

public class GlobalBibtexKeyPattern extends AbstractBibtexKeyPattern {

    private List<String> defaultBibtexKeyPattern;

    public GlobalBibtexKeyPattern(List<String> bibtexKeyPattern) {
        defaultBibtexKeyPattern = bibtexKeyPattern;
    }

    @Override
    public List<String> getLastLevelBibtexKeyPattern(String key) {
        return defaultBibtexKeyPattern;
    }
}
