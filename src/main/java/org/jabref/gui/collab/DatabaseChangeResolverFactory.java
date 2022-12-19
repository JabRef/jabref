package org.jabref.gui.collab;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.collab.entrychange.EntryChange;
import org.jabref.gui.collab.entrychange.EntryChangeResolver;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.BibEntryPreferences;

public class DatabaseChangeResolverFactory {
    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final BibEntryPreferences bibEntryPreferences;

    public DatabaseChangeResolverFactory(DialogService dialogService, BibDatabaseContext databaseContext, BibEntryPreferences bibEntryPreferences) {
        this.dialogService = dialogService;
        this.databaseContext = databaseContext;
        this.bibEntryPreferences = bibEntryPreferences;
    }

    public Optional<DatabaseChangeResolver> create(DatabaseChange change) {
        if (change instanceof EntryChange entryChange) {
            return Optional.of(new EntryChangeResolver(entryChange, dialogService, databaseContext, bibEntryPreferences));
        }

        return Optional.empty();
    }
}
