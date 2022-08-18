package org.jabref.gui.collab.experimental.entrychange;

import org.jabref.gui.collab.experimental.ExternalChange;
import org.jabref.gui.collab.experimental.ExternalChangeResolverFactory;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public final class EntryChange extends ExternalChange {
    private final BibEntry oldEntry;
    private final BibEntry newEntry;

    public EntryChange(BibEntry oldEntry, BibEntry newEntry, BibDatabaseContext databaseContext, NamedCompound undoEdit, ExternalChangeResolverFactory externalChangeResolverFactory) {
        super(databaseContext, undoEdit, externalChangeResolverFactory);
        this.oldEntry = oldEntry;
        this.newEntry = newEntry;
        setChangeName(oldEntry.getCitationKey().map(key -> Localization.lang("Modified entry '%0'", key))
                           .orElse(Localization.lang("Modified entry")));
    }

    @Override
    public void applyChange() {
        databaseContext.getDatabase().removeEntry(oldEntry);
        databaseContext.getDatabase().insertEntry(newEntry);
        undoEdit.addEdit(new UndoableInsertEntries(databaseContext.getDatabase(), oldEntry));
        undoEdit.addEdit(new UndoableInsertEntries(databaseContext.getDatabase(), newEntry));
    }
}
