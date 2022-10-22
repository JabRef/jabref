package org.jabref.gui.collab;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.theme.ThemeManager;
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
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final StateManager stateManager;
    private final ThemeManager themeManager;

    private final DatabaseChangeResolverFactory databaseChangeResolverFactory;

    public ChangeScanner(BibDatabaseContext database,
                         DialogService dialogService,
                         PreferencesService preferencesService,
                         StateManager stateManager,
                         ThemeManager themeManager) {
        this.database = database;
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.stateManager = stateManager;
        this.themeManager = themeManager;
        this.databaseChangeResolverFactory = new DatabaseChangeResolverFactory(dialogService, database);
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
