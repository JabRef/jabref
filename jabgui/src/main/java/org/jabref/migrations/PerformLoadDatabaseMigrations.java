package org.jabref.migrations;

import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

public class PerformLoadDatabaseMigrations {

    /// Runs the given migrations before the parsed library is attached to GUI listeners.
    // [impl->req~import.bibtex.legacy-migrations.only-when-needed~1]
    public static void performLoadDatabaseMigrations(ParserResult parserResult, List<PostOpenMigration> migrations) {
        List<PostOpenMigration> necessaryMigrations = migrations.stream()
                                                                .filter(migration -> migration.isMigrationNecessary(parserResult))
                                                                .toList();
        if (necessaryMigrations.isEmpty()) {
            return;
        }

        // Before the library is attached to the GUI, detach the extractor-backed entries list so each migrated
        // field does not trigger a scan of the whole library. Reinsert the same entries to rebuild its indices.
        BibDatabase database = parserResult.getDatabase();
        List<BibEntry> entries = database.getEntriesSnapshot();
        database.removeEntries(entries);
        try {
            for (PostOpenMigration migration : necessaryMigrations) {
                migration.performMigration(parserResult, entries);
            }
        } finally {
            database.insertEntries(entries);
        }
        parserResult.setChangedOnMigration(true);
    }
}
