package org.jabref.gui.collab.entrydelete;

import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeResolverFactory;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableRemoveEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.google.common.base.MoreObjects;

public final class EntryDelete extends DatabaseChange {
    private final BibEntry deletedEntry;

    public EntryDelete(BibEntry deletedEntry, BibDatabaseContext databaseContext, DatabaseChangeResolverFactory databaseChangeResolverFactory) {
        super(databaseContext, databaseChangeResolverFactory);
        this.deletedEntry = deletedEntry;
        setChangeName(deletedEntry.getCitationKey()
                           .map(key -> Localization.lang("Deleted entry '%0'", key))
                           .orElse(Localization.lang("Deleted entry")));
    }

    @Override
    public void applyChange(NamedCompound undoEdit) {
        databaseContext.getDatabase().removeEntry(deletedEntry);
        undoEdit.addEdit(new UndoableRemoveEntries(databaseContext.getDatabase(), deletedEntry));
    }

    public BibEntry getDeletedEntry() {
        return deletedEntry;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("deletedEntry", deletedEntry)
                          .toString();
    }
}
