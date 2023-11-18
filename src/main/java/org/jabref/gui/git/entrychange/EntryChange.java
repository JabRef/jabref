package org.jabref.gui.git.entrychange;

import javax.swing.undo.CompoundEdit;

import org.jabref.gui.git.GitChange;
import org.jabref.gui.git.GitChangeResolverFactory;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.undo.UndoableRemoveEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.git.BibGitContext;
import org.jabref.model.entry.BibEntry;

public final class EntryChange extends GitChange {
    private final BibEntry oldEntry;
    private final BibEntry newEntry;
    private BibGitContext gitContext;

    public EntryChange(BibEntry oldEntry, BibEntry newEntry, BibGitContext gitContext, GitChangeResolverFactory gitChangeResolverFactory) {
        super(gitContext, gitChangeResolverFactory);
        this.oldEntry = oldEntry;
        this.newEntry = newEntry;
        this.gitContext = gitContext;
        setChangeName(oldEntry.getCitationKey().map(key -> Localization.lang("Modified entry '%0'", key))
                           .orElse(Localization.lang("Modified entry")));
    }

    public EntryChange(BibEntry oldEntry, BibEntry newEntry, BibGitContext gitContext) {
        this(oldEntry, newEntry, gitContext, null);
    }

    public BibEntry getOldEntry() {
        return oldEntry;
    }

    public BibEntry getNewEntry() {
        return newEntry;
    }

    @Override
    public void applyChange(NamedCompound undoEdit) {
        this.gitContext.getDatabase().removeEntry(oldEntry);
        this.gitContext.getDatabase().insertEntry(newEntry);
        CompoundEdit changeEntryEdit = new CompoundEdit();
        changeEntryEdit.addEdit(new UndoableRemoveEntries(gitContext.getDatabase(), oldEntry));
        changeEntryEdit.addEdit(new UndoableInsertEntries(gitContext.getDatabase(), newEntry));
        changeEntryEdit.end();

        undoEdit.addEdit(changeEntryEdit);
    }
}
