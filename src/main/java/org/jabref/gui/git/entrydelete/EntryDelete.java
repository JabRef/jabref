package org.jabref.gui.collab.entrydelete;

import org.jabref.gui.collab.GitChange;
import org.jabref.gui.collab.GitChangeResolverFactory;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableRemoveEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.GitContext;
import org.jabref.model.entry.BibEntry;

public final class EntryDelete extends GitChange {
    private final BibEntry deletedEntry;

    public EntryDelete(BibEntry deletedEntry, GitContext databaseContext, GitChangeResolverFactory databaseChangeResolverFactory) {
        super(databaseContext, databaseChangeResolverFactory);
        this.deletedEntry = deletedEntry;
        setChangeName(deletedEntry.getCitationKey()
                           .map(key -> Localization.lang("Deleted entry '%0'", key))
                           .orElse(Localization.lang("Deleted entry")));
    }

    @Override
    public void applyChange(NamedCompound undoEdit) {
        databaseContext.getGit().removeEntry(deletedEntry);
        undoEdit.addEdit(new UndoableRemoveEntries(databaseContext.getGit(), deletedEntry));
    }

    public BibEntry getDeletedEntry() {
        return deletedEntry;
    }
}
