package org.jabref.gui.collab;

import java.io.IOException;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyDirectoryUpdateMonitor;
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
                         GuiPreferences preferences) {
        this.database = database;
        this.preferences = preferences;
        this.databaseChangeResolverFactory = new DatabaseChangeResolverFactory(dialogService, database, preferences);
    }

    public List<DatabaseChange> scanForChanges() {
        if (database.getDatabasePath().isEmpty()) {
            return List.of();
        }

        try {
            // Parse the modified file
            // Important: apply all post-load actions
            ImportFormatPreferences importFormatPreferences = preferences.getImportFormatPreferences();
            ParserResult result = OpenDatabase.loadDatabase(database.getDatabasePath().get(), importFormatPreferences, new DummyFileUpdateMonitor(), new DummyDirectoryUpdateMonitor());
            BibDatabaseContext databaseOnDisk = result.getDatabaseContext();

            return DatabaseChangeList.compareAndGetChanges(database, databaseOnDisk, databaseChangeResolverFactory);
        } catch (IOException e) {
            LOGGER.warn("Error while parsing changed file.", e);
            return List.of();
        }
    }
}
