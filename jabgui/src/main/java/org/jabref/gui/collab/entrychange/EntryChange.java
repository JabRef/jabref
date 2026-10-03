package org.jabref.gui.collab.entrychange;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.SequencedSet;

import org.jabref.gui.collab.DatabaseChange;
import org.jabref.gui.collab.DatabaseChangeResolverFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.undo.CompoundEdit;
import org.jabref.model.undo.UndoableChangeType;
import org.jabref.model.undo.UndoableChangedFlag;
import org.jabref.model.undo.UndoableCommentsChange;
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
    @Override
    public void applyChange(CompoundEdit undoEdit) {
        // First in the compound, so that undo restores it after the field edits have marked the entry changed again
        undoEdit.applyEdit(UndoableChangedFlag.marking(oldEntry));
        if (!oldEntry.getType().equals(newEntry.getType())) {
            undoEdit.applyEdit(new UndoableChangeType(oldEntry, oldEntry.getType(), newEntry.getType()));
        }
        SequencedSet<Field> fields = new LinkedHashSet<>(oldEntry.getFields());
        fields.addAll(newEntry.getFields());
        for (Field field : fields) {
            Optional<String> before = oldEntry.getField(field);
            Optional<String> after = newEntry.getField(field);
            if (!before.equals(after)) {
                undoEdit.applyEdit(new UndoableFieldChange(oldEntry, field, before.orElse(null), after.orElse(null)));
            }
        }
        if (!oldEntry.getUserComments().equals(newEntry.getUserComments())) {
            undoEdit.applyEdit(new UndoableCommentsChange(oldEntry, oldEntry.getUserComments(), newEntry.getUserComments()));
        }
    }
}
