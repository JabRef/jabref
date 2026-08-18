package org.jabref.gui.collab.entrychange;

import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeResolverFactory;
import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.change.UndoableInsertEntries;
import org.jabref.model.change.UndoableRemoveEntries;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

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
    public void applyChange(NamedCompoundEdit undoEdit) {
        databaseContext.getDatabase().removeEntry(oldEntry);
        databaseContext.getDatabase().insertEntry(newEntry);
        NamedCompoundEdit changeEntryEdit = new NamedCompoundEdit(getName());
        changeEntryEdit.addEdit(new UndoableRemoveEntries(databaseContext.getDatabase(), oldEntry));
        changeEntryEdit.addEdit(new UndoableInsertEntries(databaseContext.getDatabase(), newEntry));

        undoEdit.addEdit(changeEntryEdit);
    }
}
