package org.jabref.logic.importer;

import java.util.Collection;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;

public interface ImportCleanup {

    static ImportCleanup targeting(BibDatabaseMode mode) {
        return switch (mode) {
            case BIBTEX -> new ImportCleanupBibtex();
            case BIBLATEX -> new ImportCleanupBiblatex();
        };
    }

    BibEntry doPostCleanup(BibEntry entry);

    /**
     * Performs a format conversion of the given entry collection into the targeted format.
     */
    default void doPostCleanup(Collection<BibEntry> entries) {
        entries.forEach(this::doPostCleanup);
    }
}
