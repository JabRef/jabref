package org.jabref.logic.linkedfile;

import org.jabref.model.entry.types.EntryType;

public class GlobalLinkedFileNamePatterns extends AbstractLinkedFileNamePatterns {

    public GlobalLinkedFileNamePatterns(LinkedFileNamePattern defaultPattern) {
        this.defaultPattern = defaultPattern;
    }

    public static GlobalLinkedFileNamePatterns fromPattern(String pattern) {
        return new GlobalLinkedFileNamePatterns(new LinkedFileNamePattern(pattern));
    }

    @Override
    public LinkedFileNamePattern getLastLevelLinkedFileNamePattern(EntryType entryType) {
        return defaultPattern;
    }
}
