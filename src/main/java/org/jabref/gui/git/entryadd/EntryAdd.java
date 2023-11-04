package org.jabref.gui.git.entryadd;

import org.jabref.gui.git.GitChange;
import org.jabref.gui.git.GitChangeResolverFactory;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.GitContext;
import org.jabref.model.entry.BibEntry;

public final class EntryAdd extends GitChange {
    private final BibEntry addedEntry;

    public EntryAdd(BibEntry addedEntry, GitContext databaseContext, GitChangeResolverFactory databaseChangeResolverFactory) {
        super(databaseContext, databaseChangeResolverFactory);
        this.addedEntry = addedEntry;
        setChangeName(addedEntry.getCitationKey()
                           .map(key -> Localization.lang("Added entry '%0'", key))
                           .orElse(Localization.lang("Added entry")));
    }

    @Override
    public void applyChange(NamedCompound undoEdit) {
        databaseContext.getGit().insertEntry(addedEntry);
        undoEdit.addEdit(new UndoableInsertEntries(databaseContext.getGit(), addedEntry));
    }

    public BibEntry getAddedEntry() {
        return addedEntry;
    }
}
