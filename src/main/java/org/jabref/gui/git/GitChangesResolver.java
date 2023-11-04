package org.jabref.gui.git;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.collab.entrychange.EntryChangeResolver;

public sealed abstract class GitChangesResolver permits EntryChangeResolver {
    protected final DialogService dialogService;

    protected GItChangeResolver(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    public abstract Optional<GitChange> askUserToResolveChange();
}
