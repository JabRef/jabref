package org.jabref.gui.git.entryadd;

import org.jabref.gui.git.GitChange;
import org.jabref.gui.git.GitChangeResolverFactory;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.git.BibGitContext;

public final class EntryAdd extends GitChange {
    private final BibEntry addedEntry;
    private final BibGitContext databaseContext;

    public EntryAdd(BibEntry addedEntry, BibGitContext databaseContext, GitChangeResolverFactory GitChangeResolverFactory) {
        super(databaseContext, GitChangeResolverFactory);
        this.addedEntry = addedEntry;
        setChangeName(addedEntry.getCitationKey()
                           .map(key -> Localization.lang("Added entry '%0'", key))
                           .orElse(Localization.lang("Added entry")));
        this.databaseContext = databaseContext;
    }

    @Override
    public void applyChange(NamedCompound undoEdit) {
        databaseContext.getDatabase().insertEntry(addedEntry);
        undoEdit.addEdit(new UndoableInsertEntries(databaseContext.getDatabase(), addedEntry));
    }

    public BibEntry getAddedEntry() {
        return addedEntry;
    }
}
