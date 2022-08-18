package org.jabref.gui.collab.experimental;

import org.jabref.gui.DialogService;
import org.jabref.gui.collab.experimental.entrychange.EntryChangeResolver;

public sealed abstract class ExternalChangeResolver permits EntryChangeResolver {
    private final DialogService dialogService;

    protected ExternalChangeResolver(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    public abstract ExternalChange askUserToResolveChange();
}
