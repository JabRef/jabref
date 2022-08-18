package org.jabref.gui.collab.experimental;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.collab.experimental.entrychange.EntryChange;
import org.jabref.gui.collab.experimental.entrychange.EntryChangeResolver;
import org.jabref.model.database.BibDatabaseContext;

public class ExternalChangeResolverFactory {
    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;

    public ExternalChangeResolverFactory(DialogService dialogService, BibDatabaseContext databaseContext) {
        this.dialogService = dialogService;
        this.databaseContext = databaseContext;
    }

    public Optional<ExternalChangeResolver> create(ExternalChange change) {
        if (change instanceof EntryChange entryChange) {
            return Optional.of(new EntryChangeResolver(entryChange, dialogService, databaseContext));
        } else {
            return Optional.empty();
        }
    }
}
