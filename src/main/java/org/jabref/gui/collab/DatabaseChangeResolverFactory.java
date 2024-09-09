package org.jabref.gui.collab;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.collab.entrychange.EntryChange;
import org.jabref.gui.collab.entrychange.EntryChangeResolver;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.Preferences;

public class DatabaseChangeResolverFactory {
    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final Preferences preferences;

    public DatabaseChangeResolverFactory(DialogService dialogService, BibDatabaseContext databaseContext, Preferences preferences) {
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
