package org.jabref.gui.collab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jabref.gui.StateManager;
import org.jabref.logic.bibtex.comparator.BibDatabaseDiff;
import org.jabref.logic.bibtex.comparator.BibEntryDiff;
import org.jabref.logic.bibtex.comparator.BibStringDiff;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.OpenDatabase;
import org.jabref.logic.importer.ParserResult;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.GeneralPreferences;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeScanner.class);
    private final BibDatabaseContext database;
    private final PreferencesService preferencesService;
    private final StateManager stateManager;

    public ChangeScanner(BibDatabaseContext database, PreferencesService preferencesService, StateManager stateManager) {
        this.database = database;
        this.preferencesService = preferencesService;
        this.stateManager = stateManager;
    }

    public List<DatabaseChangeViewModel> scanForChanges() {
        if (database.getDatabasePath().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<DatabaseChangeViewModel> changes = new ArrayList<>();

            // Parse the modified file
            // Important: apply all post-load actions
            ImportFormatPreferences importFormatPreferences = preferencesService.getImportFormatPreferences();
            GeneralPreferences generalPreferences = preferencesService.getGeneralPreferences();
            ParserResult result = OpenDatabase.loadDatabase(database.getDatabasePath().get(), generalPreferences, importFormatPreferences, new DummyFileUpdateMonitor());
            BibDatabaseContext databaseOnDisk = result.getDatabaseContext();

            // Start looking at changes.
            BibDatabaseDiff differences = BibDatabaseDiff.compare(database, databaseOnDisk);
            differences.getMetaDataDifferences().ifPresent(diff -> {
                changes.add(new MetaDataChangeViewModel(diff, preferencesService));
                diff.getGroupDifferences().ifPresent(groupDiff -> changes.add(new GroupChangeViewModel(groupDiff)));
            });
            differences.getPreambleDifferences().ifPresent(diff -> changes.add(new PreambleChangeViewModel(diff)));
            differences.getBibStringDifferences().forEach(diff -> changes.add(createBibStringDiff(diff)));
            differences.getEntryDifferences().forEach(diff -> changes.add(createBibEntryDiff(diff)));
            return changes;
        } catch (IOException e) {
            LOGGER.warn("Error while parsing changed file.", e);
            return Collections.emptyList();
        }
    }

    private DatabaseChangeViewModel createBibStringDiff(BibStringDiff diff) {
        if (diff.getOriginalString() == null) {
            return new StringAddChangeViewModel(diff.getNewString());
        }

        if (diff.getNewString() == null) {
            return new StringRemoveChangeViewModel(diff.getOriginalString());
        }

        if (diff.getOriginalString().getName().equals(diff.getNewString().getName())) {
            return new StringChangeViewModel(diff.getOriginalString(), diff.getNewString());
        }

        return new StringNameChangeViewModel(diff.getOriginalString(), diff.getNewString());
    }

    private DatabaseChangeViewModel createBibEntryDiff(BibEntryDiff diff) {
        if (diff.getOriginalEntry() == null) {
            return new EntryAddChangeViewModel(diff.getNewEntry(), preferencesService, stateManager);
        }

        if (diff.getNewEntry() == null) {
            return new EntryDeleteChangeViewModel(diff.getOriginalEntry());
        }

        return new EntryChangeViewModel(diff.getOriginalEntry(), diff.getNewEntry());
    }
}
