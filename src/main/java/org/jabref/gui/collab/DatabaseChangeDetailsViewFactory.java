package org.jabref.gui.collab;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.collab.entryadd.EntryAdd;
import org.jabref.gui.collab.entryadd.EntryAddDetailsView;
import org.jabref.gui.collab.entrychange.EntryChange;
import org.jabref.gui.collab.entrychange.EntryChangeDetailsView;
import org.jabref.gui.collab.entrydelete.EntryDelete;
import org.jabref.gui.collab.entrydelete.EntryDeleteDetailsView;
import org.jabref.gui.collab.groupchange.GroupChange;
import org.jabref.gui.collab.groupchange.GroupChangeDetailsView;
import org.jabref.gui.collab.metedatachange.MetadataChange;
import org.jabref.gui.collab.metedatachange.MetadataChangeDetailsView;
import org.jabref.gui.collab.preamblechange.PreambleChange;
import org.jabref.gui.collab.preamblechange.PreambleChangeDetailsView;
import org.jabref.gui.collab.stringadd.BibTexStringAdd;
import org.jabref.gui.collab.stringadd.BibTexStringAddDetailsView;
import org.jabref.gui.collab.stringchange.BibTexStringChange;
import org.jabref.gui.collab.stringchange.BibTexStringChangeDetailsView;
import org.jabref.gui.collab.stringdelete.BibTexStringDelete;
import org.jabref.gui.collab.stringdelete.BibTexStringDeleteDetailsView;
import org.jabref.gui.collab.stringrename.BibTexStringRename;
import org.jabref.gui.collab.stringrename.BibTexStringRenameDetailsView;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

public class DatabaseChangeDetailsViewFactory {
    private final BibDatabaseContext databaseContext;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final ThemeManager themeManager;
    private final PreferencesService preferencesService;

    public DatabaseChangeDetailsViewFactory(BibDatabaseContext databaseContext, DialogService dialogService, StateManager stateManager, ThemeManager themeManager, PreferencesService preferencesService) {
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.themeManager = themeManager;
        this.preferencesService = preferencesService;
    }

    public DatabaseChangeDetailsView create(DatabaseChange databaseChange) {
        // TODO: Use Pattern Matching for switch once it's out of preview
        if (databaseChange instanceof EntryChange entryChange) {
            return new EntryChangeDetailsView(entryChange, databaseContext, dialogService, stateManager, themeManager, preferencesService);
        } else if (databaseChange instanceof EntryAdd entryAdd) {
            return new EntryAddDetailsView(entryAdd, databaseContext, dialogService, stateManager, themeManager, preferencesService);
        } else if (databaseChange instanceof EntryDelete entryDelete) {
            return new EntryDeleteDetailsView(entryDelete, databaseContext, dialogService, stateManager, themeManager, preferencesService);
        } else if (databaseChange instanceof BibTexStringAdd stringAdd) {
            return new BibTexStringAddDetailsView(stringAdd);
        } else if (databaseChange instanceof BibTexStringDelete stringDelete) {
            return new BibTexStringDeleteDetailsView(stringDelete);
        } else if (databaseChange instanceof BibTexStringChange stringChange) {
            return new BibTexStringChangeDetailsView(stringChange);
        } else if (databaseChange instanceof BibTexStringRename stringRename) {
            return new BibTexStringRenameDetailsView(stringRename);
        } else if (databaseChange instanceof MetadataChange metadataChange) {
            return new MetadataChangeDetailsView(metadataChange, preferencesService);
        } else if (databaseChange instanceof GroupChange groupChange) {
            return new GroupChangeDetailsView(groupChange);
        } else if (databaseChange instanceof PreambleChange preambleChange) {
            return new PreambleChangeDetailsView(preambleChange);
        }
        throw new UnsupportedOperationException("Cannot preview the given change: " + databaseChange.getName());
    }
}
