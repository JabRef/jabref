package org.jabref.gui.collab;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.collab.entrychange.EntryChange;
import org.jabref.gui.collab.entrychange.EntryChangeResolver;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.database.BibDatabaseContext;

public class DatabaseChangeResolverFactory {
    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final GuiPreferences preferences;

    public DatabaseChangeResolverFactory(DialogService dialogService, BibDatabaseContext databaseContext, GuiPreferences preferences) {
        this.dialogService = dialogService;
        this.databaseContext = databaseContext;
        this.preferences = preferences;
    }

    public Optional<DatabaseChangeResolver> create(DatabaseChange change) {
        if (change instanceof EntryChange entryChange) {
            return Optional.of(new EntryChangeResolver(entryChange, dialogService, databaseContext, preferences));
        }

        return Optional.empty();
    }
}
