package org.jabref.gui.collab.experimental;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.collab.experimental.entrychange.EntryChange;
import org.jabref.gui.collab.experimental.entrychange.EntryChangeResolver;

public class ExternalChangeResolverFactory {
    private final DialogService dialogService;

    public ExternalChangeResolverFactory(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    public Optional<ExternalChangeResolver> create(ExternalChange change) {
        if (change instanceof EntryChange) {
            return Optional.of(new EntryChangeResolver(dialogService));
        } else {
            return Optional.empty();
        }
    }
}
