package org.jabref.gui.collab.entrydelete;

import org.jabref.gui.collab.ExternalChange;
import org.jabref.gui.collab.ExternalChangeResolverFactory;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableRemoveEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public final class EntryDelete extends ExternalChange {
    private final BibEntry deletedEntry;

    public EntryDelete(BibEntry deletedEntry, BibDatabaseContext databaseContext, ExternalChangeResolverFactory externalChangeResolverFactory) {
        super(databaseContext, externalChangeResolverFactory);
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
}
