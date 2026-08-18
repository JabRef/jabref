package org.jabref.model.change;

import java.util.Objects;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.EntryType;

import org.jspecify.annotations.NullMarked;

/// A change of an entry's type.
///
/// The types are held as [EntryType], not as the strings a [org.jabref.model.FieldChange]
/// would carry, so that undo does not depend on re-parsing a display value.
@NullMarked
public record EntryTypeEdit(BibEntry entry, EntryType before, EntryType after) implements BibChange {

    @Override
    public EntryTypeEdit inverted() {
        return new EntryTypeEdit(entry, after, before);
    }

    @Override
    public void applyTo(BibDatabaseContext context) {
        entry.setType(after);
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof EntryTypeEdit other)
                && ChangeIdentity.same(entry, other.entry)
                && before.equals(other.before)
                && after.equals(other.after);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ChangeIdentity.hash(entry), before, after);
    }
}
