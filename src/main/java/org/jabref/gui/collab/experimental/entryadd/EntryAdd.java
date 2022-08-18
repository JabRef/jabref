package org.jabref.gui.collab.experimental.entryadd;

import org.jabref.gui.collab.experimental.ExternalChange;
import org.jabref.gui.collab.experimental.ExternalChangeResolverFactory;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public final class EntryAdd extends ExternalChange {
    private final BibEntry addedEntry;

    public EntryAdd(BibEntry addedEntry, BibDatabaseContext databaseContext, ExternalChangeResolverFactory externalChangeResolverFactory) {
        super(databaseContext, externalChangeResolverFactory);
        this.addedEntry = addedEntry;
        setChangeName(addedEntry.getCitationKey()
                           .map(key -> Localization.lang("Added entry '%0'", key))
                           .orElse(Localization.lang("Added entry")));
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
