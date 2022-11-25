package org.jabref.gui.collab;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.collab.entryadd.EntryAdd;
import org.jabref.gui.collab.entrychange.EntryChange;
import org.jabref.gui.collab.entrydelete.EntryDelete;
import org.jabref.gui.collab.groupchange.GroupChange;
import org.jabref.gui.collab.metedatachange.MetadataChange;
import org.jabref.gui.collab.preamblechange.PreambleChange;
import org.jabref.gui.collab.stringadd.BibTexStringAdd;
import org.jabref.gui.collab.stringchange.BibTexStringChange;
import org.jabref.gui.collab.stringdelete.BibTexStringDelete;
import org.jabref.gui.collab.stringrename.BibTexStringRename;
import org.jabref.gui.theme.ThemeManager;
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
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final StateManager stateManager;
    private final ThemeManager themeManager;

    private final ExternalChangeResolverFactory externalChangeResolverFactory;

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
        this.externalChangeResolverFactory = new ExternalChangeResolverFactory(dialogService, database);
    }

    public List<ExternalChange> scanForChanges() {
        if (database.getDatabasePath().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<ExternalChange> changes = new ArrayList<>();

            // Parse the modified file
            // Important: apply all post-load actions
            ImportFormatPreferences importFormatPreferences = preferencesService.getImportFormatPreferences();
            GeneralPreferences generalPreferences = preferencesService.getGeneralPreferences();
            ParserResult result = OpenDatabase.loadDatabase(database.getDatabasePath().get(), importFormatPreferences, new DummyFileUpdateMonitor());
            BibDatabaseContext databaseOnDisk = result.getDatabaseContext();

            // Start looking at changes.
            BibDatabaseDiff differences = BibDatabaseDiff.compare(database, databaseOnDisk);
            differences.getMetaDataDifferences().ifPresent(diff -> {
                changes.add(new MetadataChange(diff, database, externalChangeResolverFactory));
                diff.getGroupDifferences().ifPresent(groupDiff -> changes.add(new GroupChange(
                        groupDiff, database, externalChangeResolverFactory
                )));
            });
            differences.getPreambleDifferences().ifPresent(diff -> changes.add(new PreambleChange(diff, database, externalChangeResolverFactory)));
            differences.getBibStringDifferences().forEach(diff -> changes.add(createBibStringDiff(diff)));
            differences.getEntryDifferences().forEach(diff -> changes.add(createBibEntryDiff(diff)));
            return changes;
        } catch (IOException e) {
            LOGGER.warn("Error while parsing changed file.", e);
            return Collections.emptyList();
        }
    }

    private ExternalChange createBibStringDiff(BibStringDiff diff) {
         if (diff.getOriginalString() == null) {
            return new BibTexStringAdd(diff.getNewString(), database, externalChangeResolverFactory);
        }

        if (diff.getNewString() == null) {
            return new BibTexStringDelete(diff.getOriginalString(), database, externalChangeResolverFactory);
        }

        if (diff.getOriginalString().getName().equals(diff.getNewString().getName())) {
            return new BibTexStringChange(diff.getOriginalString(), diff.getNewString(), database, externalChangeResolverFactory);
        }

        return new BibTexStringRename(diff.getOriginalString(), diff.getNewString(), database, externalChangeResolverFactory);
    }

    private ExternalChange createBibEntryDiff(BibEntryDiff diff) {
        if (diff.getOriginalEntry() == null) {
            return new EntryAdd(diff.getNewEntry(), database, externalChangeResolverFactory);
        }

        if (diff.getNewEntry() == null) {
            return new EntryDelete(diff.getOriginalEntry(), database, externalChangeResolverFactory);
        }

        return new EntryChange(diff.getOriginalEntry(), diff.getNewEntry(), database, externalChangeResolverFactory);
    }
}
