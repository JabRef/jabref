package org.jabref.logic.filenameformatpatterns;


import org.jabref.model.entry.types.EntryType;

public class GlobalFilenamePattern extends AbstractFilenameFormatPatterns {

    public GlobalFilenamePattern(FilenameFormatPattern defaultPattern) {
        this.defaultPattern = defaultPattern;
    }

    public static GlobalFilenamePattern fromPattern(String pattern) {
        return new GlobalFilenamePattern(new FilenameFormatPattern(pattern));
    }

    @Override
    public FilenameFormatPattern getLastLevelFilenameFormatPattern(EntryType entryType) {
        return defaultPattern;
    }
}
