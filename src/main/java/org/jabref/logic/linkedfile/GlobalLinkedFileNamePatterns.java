package org.jabref.logic.linkedfile;

import org.jabref.logic.citationkeypattern.Pattern;
import org.jabref.model.entry.types.EntryType;

public class GlobalLinkedFileNamePatterns extends AbstractLinkedFileNamePatterns {

    public GlobalLinkedFileNamePatterns(Pattern defaultPattern) {
        this.defaultPattern = defaultPattern;
    }

    public static GlobalLinkedFileNamePatterns fromPattern(String pattern) {
        return new GlobalLinkedFileNamePatterns(new Pattern(pattern));
    }

    @Override
    public Pattern getLastLevelLinkedFileNamePattern(EntryType entryType) {
        return defaultPattern;
    }
}
