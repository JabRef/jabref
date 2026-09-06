package org.jabref.gui.collab.entrychange;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeResolverFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.undo.CompoundEdit;
import org.jabref.model.undo.UndoableChangeType;
import org.jabref.model.undo.UndoableFieldChange;

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

    /// Changes the entry in place rather than replacing it, so it keeps its identity: table position, selection, and
    /// an open entry editor stay as they are.
    ///
    /// The changes are collected in a nested step named after this change, so a failure while applying them names
    /// the entry it belongs to instead of only the enclosing merge.
    @Override
    public void applyChange(CompoundEdit undoEdit) {
        CompoundEdit entryEdit = new CompoundEdit(getName());
        if (!oldEntry.getType().equals(newEntry.getType())) {
            entryEdit.applyEdit(new UndoableChangeType(oldEntry, oldEntry.getType(), newEntry.getType()));
        }
        Set<Field> fields = new LinkedHashSet<>(oldEntry.getFields());
        fields.addAll(newEntry.getFields());
        for (Field field : fields) {
            String before = oldEntry.getField(field).orElse(null);
            String after = newEntry.getField(field).orElse(null);
            if (!Objects.equals(before, after)) {
                entryEdit.applyEdit(new UndoableFieldChange(oldEntry, field, before, after));
            }
        }
        undoEdit.addEdit(entryEdit);
    }
}
