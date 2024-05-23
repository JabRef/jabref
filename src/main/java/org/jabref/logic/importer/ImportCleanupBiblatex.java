package org.jabref.logic.importer;

import org.jabref.logic.cleanup.ConvertToBiblatexCleanup;
import org.jabref.model.entry.BibEntry;

public class ImportCleanupBiblatex implements ImportCleanup {

    private final ConvertToBiblatexCleanup convertToBiblatexCleanup = new ConvertToBiblatexCleanup();

    /**
     * Performs a format conversion of the given entry into the targeted format.
     * Modifies the given entry and also returns it to enable usage of doPostCleanup in streams.
     *
     * @return Cleaned up BibEntry
     */
    @Override
    public BibEntry doPostCleanup(BibEntry entry) {
         convertToBiblatexCleanup.cleanup(entry);
         return entry;
    }
}
