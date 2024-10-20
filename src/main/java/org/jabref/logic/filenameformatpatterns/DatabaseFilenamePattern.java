package org.jabref.logic.filenameformatpatterns;

import org.jabref.model.entry.types.EntryType;





// Not sure if this class require or not, double check


public class DatabaseFilenamePattern extends AbstractFilenameFormatPatterns {

    private final GlobalFilenamePattern globalFilenamePattern;

    public DatabaseFilenamePattern(GlobalFilenamePattern globalFilenamePattern) {
        this.globalFilenamePattern = globalFilenamePattern;
    }

    @Override
    public FilenameFormatPattern getLastLevelFilenameFormatPattern(EntryType entryType) {
        return globalFilenamePattern.getValue(entryType);
    }
}
