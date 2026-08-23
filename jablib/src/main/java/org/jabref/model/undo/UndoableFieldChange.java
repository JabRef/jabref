package org.jabref.model.undo;

import java.util.Objects;

import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// A change of one field of one entry. A `null` value means the field is absent.
@NullMarked
public record UndoableFieldChange(BibEntry entry, Field field, @Nullable String before, @Nullable String after) implements BibChange {

    public UndoableFieldChange(FieldChange change) {
        this(change.getEntry(), change.getField(), change.getOldValue(), change.getNewValue());
    }

    @Override
    public UndoableFieldChange inverted() {
        return new UndoableFieldChange(entry, field, after, before);
    }

    @Override
    public void apply() {
        if (after == null) {
            entry.clearField(field);
        } else {
            entry.setField(field, after);
        }
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof UndoableFieldChange other)
                && ChangeIdentity.same(entry, other.entry)
                && field.equals(other.field)
                && Objects.equals(before, other.before)
                && Objects.equals(after, other.after);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ChangeIdentity.hash(entry), field, before, after);
    }
}
