package org.jabref.gui.collab;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.collab.entrychange.EntryChangeResolver;

public sealed abstract class ExternalChangeResolver permits EntryChangeResolver {
    protected final DialogService dialogService;

    protected ExternalChangeResolver(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    public abstract Optional<ExternalChange> askUserToResolveChange();
}
