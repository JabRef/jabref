package org.jabref.gui.collab.experimental.entrychange;

import org.jabref.gui.collab.experimental.ExternalChangeDetailsView;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabaseContext;

public final class EntryChangeDetailsView extends ExternalChangeDetailsView {

    public EntryChangeDetailsView(String name, NamedCompound undoEdit, BibDatabaseContext bibDatabaseContext) {
        super(name, undoEdit, bibDatabaseContext);
    }
}
