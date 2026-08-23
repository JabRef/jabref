package org.jabref.gui.collab.entrychange;

import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeResolverFactory;
import org.jabref.gui.undo.CompoundEdit;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.undo.UndoableInsertEntries;
import org.jabref.model.undo.UndoableRemoveEntries;

public final class EntryChange extends DatabaseChange {
    private final BibEntry oldEntry;
    private final BibEntry newEntry;

    public EntryChange(BibEntry oldEntry, BibEntry newEntry, BibDatabaseContext databaseContext, DatabaseChangeResolverFactory databaseChangeResolverFactory) {
        super(databaseContext, databaseChangeResolverFactory);
        this.oldEntry = oldEntry;
        this.newEntry = newEntry;
        setChangeName(oldEntry.getCitationKey().map(key -> Localization.lang("Modified entry '%0'", key))
                              .orElse(Localization.lang("Modified entry")));
    }

    public EntryChange(BibEntry oldEntry, BibEntry newEntry, BibDatabaseContext databaseContext) {
        this(oldEntry, newEntry, databaseContext, null);
    }

    public BibEntry getOldEntry() {
        return oldEntry;
    }

    public BibEntry getNewEntry() {
        return newEntry;
    }

    @Override
    public void applyChange(CompoundEdit undoEdit) {
        databaseContext.getDatabase().removeEntry(oldEntry);
        databaseContext.getDatabase().insertEntry(newEntry);
        undoEdit.addEdit(new UndoableRemoveEntries(databaseContext.getDatabase(), oldEntry));
        undoEdit.addEdit(new UndoableInsertEntries(databaseContext.getDatabase(), newEntry));
    }
}
