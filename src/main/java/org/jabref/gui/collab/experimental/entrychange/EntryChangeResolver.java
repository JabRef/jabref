package org.jabref.gui.collab.experimental.entrychange;

import org.jabref.gui.collab.experimental.ExternalChangeResolver;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabaseContext;

public final class EntryChangeResolver extends ExternalChangeResolver {
    public EntryChangeResolver(String changeName, BibDatabaseContext databaseContext, NamedCompound undoEdit) {
        super(changeName, databaseContext, undoEdit);
    }

    @Override
    public void execute() {
    }
}
