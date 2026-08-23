package org.jabref.model.undo;

import java.util.Objects;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;

import org.jspecify.annotations.NullMarked;

/// A change of an entry's type.
///
/// The types are held as [EntryType], not as the strings a [org.jabref.model.FieldChange]
/// would carry, so that undo does not depend on re-parsing a display value.
@NullMarked
public record UndoableChangeType(BibEntry entry, EntryType before, EntryType after) implements BibChange {

    @Override
    public UndoableChangeType inverted() {
        return new UndoableChangeType(entry, after, before);
    }

    @Override
    public void apply() {
        entry.setType(after);
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof UndoableChangeType other)
                && ChangeIdentity.same(entry, other.entry)
                && before.equals(other.before)
                && after.equals(other.after);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ChangeIdentity.hash(entry), before, after);
    }
}
