package org.jabref.gui.collab.experimental.entrychange;

import org.jabref.gui.collab.experimental.ExternalChangeDetailsViewModel;
import org.jabref.model.database.BibDatabaseContext;

public class EntryChangeDetailsViewModel extends ExternalChangeDetailsViewModel {
    public EntryChangeDetailsViewModel(BibDatabaseContext databaseContext, String name) {
        super(databaseContext, name);
    }
}
