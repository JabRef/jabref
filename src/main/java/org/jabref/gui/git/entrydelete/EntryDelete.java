package org.jabref.gui.git.entrydelete;

import org.jabref.gui.git.GitChange;
import org.jabref.gui.git.GitChangeResolverFactory;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableRemoveEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.git.BibGitContext;
import org.jabref.model.entry.BibEntry;

public final class EntryDelete extends GitChange {
    private final BibEntry deletedEntry;
    private final BibGitContext databaseContext;

    public EntryDelete(BibEntry deletedEntry, BibGitContext databaseContext, GitChangeResolverFactory databaseChangeResolverFactory) {
        super(databaseContext, databaseChangeResolverFactory);
        this.deletedEntry = deletedEntry;
        setChangeName(deletedEntry.getCitationKey()
                           .map(key -> Localization.lang("Deleted entry '%0'", key))
                           .orElse(Localization.lang("Deleted entry")));
        this.databaseContext = databaseContext;
    }

    @Override
    public void applyChange(NamedCompound undoEdit) {
        this.databaseContext.getDatabase().removeEntry(deletedEntry);
        undoEdit.addEdit(new UndoableRemoveEntries(this.databaseContext.getDatabase(), deletedEntry));
    }

    public BibEntry getDeletedEntry() {
        return deletedEntry;
    }
}
