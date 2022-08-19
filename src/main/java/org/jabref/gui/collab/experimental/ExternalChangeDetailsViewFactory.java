package org.jabref.gui.collab.experimental;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.collab.experimental.entryadd.EntryAdd;
import org.jabref.gui.collab.experimental.entryadd.EntryAddDetailsView;
import org.jabref.gui.collab.experimental.entrychange.EntryChange;
import org.jabref.gui.collab.experimental.entrychange.EntryChangeDetailsView;
import org.jabref.gui.collab.experimental.entrydelete.EntryDelete;
import org.jabref.gui.collab.experimental.entrydelete.EntryDeleteDetailsView;
import org.jabref.gui.collab.experimental.stringadd.StringAdd;
import org.jabref.gui.collab.experimental.stringadd.StringAddDetailsView;
import org.jabref.gui.collab.experimental.stringchange.StringChange;
import org.jabref.gui.collab.experimental.stringchange.StringChangeDetailsView;
import org.jabref.gui.collab.experimental.stringdelete.StringDelete;
import org.jabref.gui.collab.experimental.stringdelete.StringDeleteDetailsView;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

public class ExternalChangeDetailsViewFactory {
    private final BibDatabaseContext databaseContext;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final ThemeManager themeManager;
    private final PreferencesService preferencesService;

    public ExternalChangeDetailsViewFactory(BibDatabaseContext databaseContext, DialogService dialogService, StateManager stateManager, ThemeManager themeManager, PreferencesService preferencesService) {
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.themeManager = themeManager;
        this.preferencesService = preferencesService;
    }

    public ExternalChangeDetailsView create(ExternalChange externalChange) {
        // TODO: Use Pattern Matching for switch once it's out of preview
        if (externalChange instanceof EntryChange entryChange) {
            return new EntryChangeDetailsView(entryChange, databaseContext, dialogService, stateManager, themeManager, preferencesService);
        } else if (externalChange instanceof EntryAdd entryAdd) {
            return new EntryAddDetailsView(entryAdd, databaseContext, dialogService, stateManager, themeManager, preferencesService);
        } else if (externalChange instanceof EntryDelete entryDelete) {
            return new EntryDeleteDetailsView(entryDelete, databaseContext, dialogService, stateManager, themeManager, preferencesService);
        } else if (externalChange instanceof StringAdd stringAdd) {
            return new StringAddDetailsView(stringAdd);
        } else if (externalChange instanceof StringDelete stringDelete) {
            return new StringDeleteDetailsView(stringDelete);
        } else if (externalChange instanceof StringChange stringChange) {
            return new StringChangeDetailsView(stringChange);
        }
        throw new UnsupportedOperationException("Cannot preview the given change: " + externalChange.getName());
    }
}
