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
        if (database.getDatabasePath().isEmpty()) {
            return List.of();
        }

        try {
            return getDatabaseChanges(database.getDatabasePath().get());
        } catch (IOException e) {
            LOGGER.warn("Error while parsing changed file.", e);
            return List.of();
        }
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
