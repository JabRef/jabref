package org.jabref.model.bibtexkeypattern;

import java.util.List;

import org.jabref.model.entry.types.EntryType;

public class DatabaseBibtexKeyPattern extends AbstractBibtexKeyPattern {

    private final GlobalBibtexKeyPattern globalBibtexKeyPattern;


    public DatabaseBibtexKeyPattern(GlobalBibtexKeyPattern globalBibtexKeyPattern) {
        this.globalBibtexKeyPattern = globalBibtexKeyPattern;
    }

    @Override
    public List<String> getLastLevelBibtexKeyPattern(EntryType entryType) {
        return globalBibtexKeyPattern.getValue(entryType);
    }

}
