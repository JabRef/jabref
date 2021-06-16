package org.jabref.logic.importer;

import java.util.Collection;

import org.jabref.logic.cleanup.ConvertToBiblatexCleanup;
import org.jabref.logic.cleanup.ConvertToBibtexCleanup;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;

public class ImportCleanup {

    private final BibDatabaseMode targetBibEntryFormat;

    public ImportCleanup(BibDatabaseMode targetBibEntryFormat) {
        this.targetBibEntryFormat = targetBibEntryFormat;
    }

    /**
     * Performs a format conversion of the given entry into the targeted format.
     *
     * @return Returns the cleaned up bibentry to enable usage of doPostCleanup in streams.
     */
    public BibEntry doPostCleanup(BibEntry entry) {
        if (targetBibEntryFormat == BibDatabaseMode.BIBTEX) {
            new ConvertToBibtexCleanup().cleanup(entry);
        } else if (targetBibEntryFormat == BibDatabaseMode.BIBLATEX) {
            new ConvertToBiblatexCleanup().cleanup(entry);
        }
        return entry;
    }

    /**
     * Performs a format conversion of the given entry collection into the targeted format.
     */
    public void doPostCleanup(Collection<BibEntry> entries) {
        entries.forEach(this::doPostCleanup);
    }
}
