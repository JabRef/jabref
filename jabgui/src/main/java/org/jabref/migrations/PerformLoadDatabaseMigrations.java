package org.jabref.migrations;

import java.util.List;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.shared.DatabaseLocation;
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

        List<PostOpenMigration> entryMigrations = necessaryMigrations.stream().filter(PostOpenMigration::modifiesEntries).toList();
        if (!entryMigrations.isEmpty()) {
            migrateEntries(parserResult, entryMigrations);
        }
        necessaryMigrations.stream()
                           .filter(migration -> !migration.modifiesEntries())
                           .forEach(migration -> migration.performMigration(parserResult, List.of()));
        parserResult.setChangedOnMigration(true);
    }

    private static void migrateEntries(ParserResult parserResult, List<PostOpenMigration> migrations) {
        if (parserResult.getDatabaseContext().getLocation() == DatabaseLocation.SHARED) {
            // Removing entries would propagate deletions to the shared database.
            List<BibEntry> entries = parserResult.getDatabase().getEntriesSnapshot();
            migrations.forEach(migration -> migration.performMigration(parserResult, entries));
            return;
        }
        // Detach the extractor-backed list to avoid scanning the whole library for each migrated field.
        BibDatabase database = parserResult.getDatabase();
        List<BibEntry> entries = database.getEntriesSnapshot();
        database.removeEntries(entries);
        try {
            for (PostOpenMigration migration : migrations) {
                migration.performMigration(parserResult, entries);
            }
        } finally {
            database.insertEntries(entries);
        }
    }
}
