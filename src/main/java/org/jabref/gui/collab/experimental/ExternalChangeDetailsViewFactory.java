package org.jabref.gui.collab.experimental;

import org.jabref.gui.collab.experimental.entrychange.EntryChangeDetailsView;
import org.jabref.gui.collab.experimental.entrychange.EntryChangeResolver;

public class ExternalChangeDetailsViewFactory {

    public ExternalChangeDetailsView create(ExternalChangeResolver resolver) {
        return switch (resolver) {
            case EntryChangeResolver entryChange -> new EntryChangeDetailsView(entryChange.getName(), null, null);
        };
    }
}
