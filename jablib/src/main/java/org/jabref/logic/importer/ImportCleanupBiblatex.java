package org.jabref.logic.importer;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.cleanup.ConvertToBiblatexCleanup;
import org.jabref.model.entry.BibEntry;

public class ImportCleanupBiblatex extends ImportCleanup {

    private final ConvertToBiblatexCleanup convertToBiblatexCleanup = new ConvertToBiblatexCleanup();

    public ImportCleanupBiblatex(FieldPreferences fieldPreferences) {
        super(fieldPreferences);
    }

    /**
     * Performs a format conversion of the given entry into the targeted format.
     * Modifies the given entry and also returns it to enable usage of doPostCleanup in streams.
     *
     * @return Cleaned up BibEntry
     */
    @Override
    public BibEntry doPostCleanup(BibEntry entry) {
        entry = super.doPostCleanup(entry);
        convertToBiblatexCleanup.cleanup(entry);
        return entry;
    }
}
