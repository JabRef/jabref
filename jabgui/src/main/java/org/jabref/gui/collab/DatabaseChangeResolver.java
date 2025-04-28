package org.jabref.gui.collab;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.collab.entrychange.EntryChangeResolver;

public sealed abstract class DatabaseChangeResolver permits EntryChangeResolver {
    protected final DialogService dialogService;

    protected DatabaseChangeResolver(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    public abstract Optional<DatabaseChange> askUserToResolveChange();
}
