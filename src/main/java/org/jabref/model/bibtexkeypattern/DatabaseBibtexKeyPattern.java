package org.jabref.model.bibtexkeypattern;

import java.util.List;

public class DatabaseBibtexKeyPattern extends AbstractBibtexKeyPattern {

    private final GlobalBibtexKeyPattern globalBibtexKeyPattern;


    public DatabaseBibtexKeyPattern(GlobalBibtexKeyPattern globalBibtexKeyPattern) {
        this.globalBibtexKeyPattern = globalBibtexKeyPattern;
    }

    @Override
    public List<String> getLastLevelBibtexKeyPattern(String key) {
        return globalBibtexKeyPattern.getValue(key);
    }

}
