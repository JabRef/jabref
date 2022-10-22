package org.jabref.gui.collab;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.collab.entrychange.EntryChange;
import org.jabref.gui.collab.entrychange.EntryChangeResolver;
import org.jabref.model.database.BibDatabaseContext;

public class DatabaseChangeResolverFactory {
    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;

    public DatabaseChangeResolverFactory(DialogService dialogService, BibDatabaseContext databaseContext) {
        this.dialogService = dialogService;
        this.databaseContext = databaseContext;
    }

    public Optional<DatabaseChangeResolver> create(DatabaseChange change) {
        if (change instanceof EntryChange entryChange) {
            return Optional.of(new EntryChangeResolver(entryChange, dialogService, databaseContext));
        }

        return Optional.empty();
    }
}
