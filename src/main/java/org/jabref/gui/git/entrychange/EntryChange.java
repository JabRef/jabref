package org.jabref.gui.collab.entrychange;

import javax.swing.undo.CompoundEdit;

import org.jabref.gui.collab.GitChange;
import org.jabref.gui.collab.GitChangeResolverFactory;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.undo.UndoableRemoveEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.GitContext;
import org.jabref.model.entry.BibEntry;

public final class EntryChange extends GitChange {
    private final BibEntry oldEntry;
    private final BibEntry newEntry;

    public EntryChange(BibEntry oldEntry, BibEntry newEntry, GitContext databaseContext, GitChangeResolverFactory databaseChangeResolverFactory) {
        super(databaseContext, databaseChangeResolverFactory);
        this.oldEntry = oldEntry;
        this.newEntry = newEntry;
        setChangeName(oldEntry.getCitationKey().map(key -> Localization.lang("Modified entry '%0'", key))
                           .orElse(Localization.lang("Modified entry")));
    }

    public EntryChange(BibEntry oldEntry, BibEntry newEntry, GitContext databaseContext) {
        this(oldEntry, newEntry, databaseContext, null);
    }

    public BibEntry getOldEntry() {
        return oldEntry;
    }

    public BibEntry getNewEntry() {
        return newEntry;
    }

    @Override
    public void applyChange(NamedCompound undoEdit) {
        databaseContext.getGit().removeEntry(oldEntry);
        databaseContext.getGit().insertEntry(newEntry);
        CompoundEdit changeEntryEdit = new CompoundEdit();
        changeEntryEdit.addEdit(new UndoableRemoveEntries(databaseContext.getGit(), oldEntry));
        changeEntryEdit.addEdit(new UndoableInsertEntries(databaseContext.getGit(), newEntry));
        changeEntryEdit.end();

        undoEdit.addEdit(changeEntryEdit);
    }
}
