package org.jabref.gui.collab.experimental.entrychange;

import org.jabref.gui.DialogService;
import org.jabref.gui.collab.experimental.ExternalChange;
import org.jabref.gui.collab.experimental.ExternalChangeResolver;

public final class EntryChangeResolver extends ExternalChangeResolver {
    public EntryChangeResolver(DialogService dialogService) {
        super(dialogService);
    }

    @Override
    public ExternalChange askUserToResolveChange() {
        return null;
    }
}
