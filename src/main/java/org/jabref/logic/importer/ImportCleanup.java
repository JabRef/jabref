package org.jabref.logic.importer;

import java.util.Collection;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.cleanup.NormalizeWhitespacesCleanup;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NonNull;

/**
 * Cleanup of imported entries to be processable by JabRef
 */
public abstract class ImportCleanup {

    private final NormalizeWhitespacesCleanup normalizeWhitespacesCleanup;

    protected ImportCleanup(FieldPreferences fieldPreferences) {
        this.normalizeWhitespacesCleanup = new NormalizeWhitespacesCleanup(fieldPreferences);
    }

    /**
     * Kind of builder for a cleanup
     */
    public static ImportCleanup targeting(BibDatabaseMode mode, @NonNull FieldPreferences fieldPreferences) {
        return switch (mode) {
            case BIBTEX -> new ImportCleanupBibtex(fieldPreferences);
            case BIBLATEX -> new ImportCleanupBiblatex(fieldPreferences);
        };
    }

    /**
     * @implNote Related method: {@link ParserFetcher#doPostCleanup(BibEntry)}
     */
    public BibEntry doPostCleanup(BibEntry entry) {
        normalizeWhitespacesCleanup.cleanup(entry);
        return entry;
    }

    /**
     * Performs a format conversion of the given entry collection into the targeted format.
     */
    public void doPostCleanup(Collection<BibEntry> entries) {
        entries.forEach(this::doPostCleanup);
    }
}
