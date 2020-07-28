package org.jabref.logic.importer;

import java.util.Collection;

import org.jabref.logic.cleanup.ConvertToBiblatexCleanup;
import org.jabref.logic.cleanup.ConvertToBibtexCleanup;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;

public class ImportCleanup {

    /**
     * Performs a format conversion of the given entry into the targeted format.
     */
    public void doPostCleanup(BibEntry entry, BibDatabaseMode targetBibEntryFormat) {
        if (targetBibEntryFormat == BibDatabaseMode.BIBTEX) {
            new ConvertToBibtexCleanup().cleanup(entry);
        } else if (targetBibEntryFormat == BibDatabaseMode.BIBLATEX) {
            new ConvertToBiblatexCleanup().cleanup(entry);
        }
    }

    /**
     * Performs a format conversion of the given entry collection into the targeted format.
     */
    public void doPostCleanup(Collection<BibEntry> entries, BibDatabaseMode targetBibEntryFormat) {
        entries.parallelStream().forEach(entry -> doPostCleanup(entry, targetBibEntryFormat));
    }
}
