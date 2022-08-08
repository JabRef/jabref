package org.jabref.gui.collab.entrychange;

import org.jabref.gui.collab.ExternalChangeResolver;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabaseContext;

public class EntryChangeResolver extends ExternalChangeResolver {
    protected EntryChangeResolver(BibDatabaseContext databaseContext, NamedCompound undoEdit) {
        super(databaseContext, undoEdit);
    }

    @Override
    public void execute() {
    }
}
