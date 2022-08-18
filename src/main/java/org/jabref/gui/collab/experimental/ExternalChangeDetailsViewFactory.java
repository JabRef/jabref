package org.jabref.gui.collab.experimental;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.collab.experimental.entrychange.EntryChange;
import org.jabref.gui.collab.experimental.entrychange.EntryChangeDetailsView;
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
        if (externalChange instanceof EntryChange entryChange) {
            return new EntryChangeDetailsView(entryChange, databaseContext, dialogService, stateManager, themeManager, preferencesService);
        }
        throw new UnsupportedOperationException("No implementation found for the given change preview");
    }
}
