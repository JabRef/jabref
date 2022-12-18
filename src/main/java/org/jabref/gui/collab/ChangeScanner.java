package org.jabref.gui.collab;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeScanner.class);
    private final BibDatabaseContext database;
    private final PreferencesService preferencesService;

    private final DatabaseChangeResolverFactory databaseChangeResolverFactory;

    public ChangeScanner(BibDatabaseContext database,
                         DialogService dialogService,
                         PreferencesService preferencesService) {
        this.database = database;
        this.preferencesService = preferencesService;
        this.databaseChangeResolverFactory = new DatabaseChangeResolverFactory(dialogService, database, preferencesService.getBibEntryPreferences());
    }

    public List<DatabaseChange> scanForChanges() {
        if (database.getDatabasePath().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // Parse the modified file
            // Important: apply all post-load actions
            ImportFormatPreferences importFormatPreferences = preferencesService.getImportFormatPreferences();
            ParserResult result = OpenDatabase.loadDatabase(database.getDatabasePath().get(), importFormatPreferences, new DummyFileUpdateMonitor());
            BibDatabaseContext databaseOnDisk = result.getDatabaseContext();

            return DatabaseChangeList.compareAndGetChanges(database, databaseOnDisk, databaseChangeResolverFactory);
        } catch (IOException e) {
            LOGGER.warn("Error while parsing changed file.", e);
            return Collections.emptyList();
        }
    }
}
