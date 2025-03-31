package org.jabref.logic.linkedfile;

import org.jabref.logic.citationkeypattern.KeyPattern;
import org.jabref.model.entry.types.EntryType;

public class GlobalLinkedFileNamePatterns extends AbstractLinkedFileNamePatterns {

    public GlobalLinkedFileNamePatterns(KeyPattern defaultPattern) {
        this.defaultPattern = defaultPattern;
    }

    public static GlobalLinkedFileNamePatterns fromPattern(String pattern) {
        return new GlobalLinkedFileNamePatterns(new KeyPattern(pattern));
    }

    @Override
    public KeyPattern getLastLevelLinkedFileNamePattern(EntryType entryType) {
        return defaultPattern;
    }
}
