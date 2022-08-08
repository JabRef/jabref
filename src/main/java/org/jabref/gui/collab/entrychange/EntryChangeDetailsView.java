package org.jabref.gui.collab.entrychange;

import org.jabref.gui.collab.ExternalChangeDetailsView;
import org.jabref.gui.collab.ExternalChangeResolver;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabaseContext;

public class EntryChangeDetailsView extends ExternalChangeDetailsView {

    public EntryChangeDetailsView(String name, NamedCompound undoEdit, BibDatabaseContext bibDatabaseContext) {
        super(name, undoEdit, bibDatabaseContext);
    }

    @Override
    public ExternalChangeResolver getExternalChangeResolver() {
        return null;
    }
}
