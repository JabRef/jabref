package org.jabref.gui.collab.experimental;

import org.jabref.gui.collab.experimental.entrychange.EntryChange;
import org.jabref.gui.collab.experimental.entrychange.EntryChangeDetailsView;

public class ExternalChangeDetailsViewFactory {

    public ExternalChangeDetailsView create(ExternalChange externalChange) {
        if (externalChange instanceof EntryChange entryChange) {
            return new EntryChangeDetailsView(entryChange, null, null, null, null);
        }
        throw new UnsupportedOperationException("No implementation found for the given change preview");
    }
}
