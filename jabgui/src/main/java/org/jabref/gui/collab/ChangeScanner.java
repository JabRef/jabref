package org.jabref.gui.collab;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeScanner.class);
    private final BibDatabaseContext database;
    private final GuiPreferences preferences;

    private final DatabaseChangeResolverFactory databaseChangeResolverFactory;

    public ChangeScanner(BibDatabaseContext database,
                         DialogService dialogService,
                         GuiPreferences preferences,
                         StateManager stateManager) {
        this.database = database;
        this.preferences = preferences;
        this.databaseChangeResolverFactory = new DatabaseChangeResolverFactory(dialogService, database, preferences, stateManager);
    }

    public List<DatabaseChange> scanForChanges() {
        return scanForChanges(() -> {
        });
    }

    /// @param beforeParsing run right before the file is parsed, e.g. to wait until a sync client has finished writing it
    public List<DatabaseChange> scanForChanges(Runnable beforeParsing) {
        if (database.getDatabasePath().isEmpty()) {
            return List.of();
        }
        beforeParsing.run();

        try {
            return getDatabaseChanges(database.getDatabasePath().get());
        } catch (IOException e) {
            LOGGER.warn("Error while parsing changed file.", e);
            return List.of();
        }
    }

    /// The differences between the in-memory library and the given file, e.g. a conflicted copy left by a sync client.
    ///
    /// @throws IOException when the file cannot be read or parsed; unlike for the library file itself, an unreadable copy must not pass as "nothing to merge"
    public List<DatabaseChange> scanFile(Path file) throws IOException {
        ParserResult result = OpenDatabase.loadDatabase(file, preferences.getImportFormatPreferences(), new DummyFileUpdateMonitor());
        if (result.isInvalid()) {
            throw new IOException("Could not parse " + file + ": " + result.getErrorMessage());
        }
        return DatabaseChangeList.compareAndGetChanges(database, result.getDatabaseContext(), databaseChangeResolverFactory);
    }

    /// @return the given external changes sorted by the side they happened on, see [LibraryBaseline#triage]
    public LibraryBaseline.Triage triage(LibraryBaseline baseline, List<DatabaseChange> changes) {
        return baseline.triage(changes, database, databaseChangeResolverFactory);
    }

    public List<DatabaseChange> getDatabaseChanges(Path fileToCompare) throws IOException {
        ImportFormatPreferences importFormatPreferences = preferences.getImportFormatPreferences();
        ParserResult result = OpenDatabase.loadDatabase(fileToCompare, importFormatPreferences, new DummyFileUpdateMonitor());

        if (result.isInvalid() || result.isEmpty()) {
            return List.of();
        }

        BibDatabaseContext databaseOnDisk = result.getDatabaseContext();
        return DatabaseChangeList.compareAndGetChanges(database, databaseOnDisk, databaseChangeResolverFactory);
    }
}
