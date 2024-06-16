package org.jabref.logic.importer;

import java.util.Collection;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;

/**
 * Cleanup of imported entries to be processable by JabRef
 */
public abstract class ImportCleanup {

    // TODO: Let "fieldPreferences" be passed through the constructor
    // private final NormalizeWhitespacesCleanup normalizeWhitespacesCleanup = new NormalizeWhitespacesCleanup(fieldPreferences);

    /**
     * Kind of builder for a cleanup
     */
    public static ImportCleanup targeting(BibDatabaseMode mode) {
        return switch (mode) {
            case BIBTEX -> new ImportCleanupBibtex();
            case BIBLATEX -> new ImportCleanupBiblatex();
        };
    }

    /**
     * @implNote Related method: {@link ParserFetcher#doPostCleanup(BibEntry)}
     */
    public BibEntry doPostCleanup(BibEntry entry) {
        // TODO normalizeWhitespacesCleanup(entry);
        return entry;
    }

    /**
     * Performs a format conversion of the given entry collection into the targeted format.
     */
    public void doPostCleanup(Collection<BibEntry> entries) {
        entries.forEach(this::doPostCleanup);
    }
}
