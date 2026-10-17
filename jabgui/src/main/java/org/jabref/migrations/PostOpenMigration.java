package org.jabref.migrations;

import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.entry.BibEntry;

public interface PostOpenMigration {
    boolean isMigrationNecessary(ParserResult parserResult);

    default void performMigration(ParserResult parserResult) {
        performMigration(parserResult, parserResult.getDatabase().getEntries());
    }

    /// Entries may be temporarily detached from the database to avoid per-field observable-list scans.
    void performMigration(ParserResult parserResult, List<BibEntry> entries);
}
