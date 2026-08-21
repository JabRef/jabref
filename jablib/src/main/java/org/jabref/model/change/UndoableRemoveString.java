package org.jabref.model.change;

import java.util.Objects;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibtexString;

import org.jspecify.annotations.NullMarked;

/// Removal of a BibTeX string definition.
@NullMarked
public record UndoableRemoveString(BibDatabase database, BibtexString string) implements BibChange {

    @Override
    public UndoableInsertString inverted() {
        return new UndoableInsertString(database, string);
    }

    @Override
    public void apply() {
        database.removeString(string.getId());
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof UndoableRemoveString other)
                && ChangeIdentity.same(database, other.database)
                && ChangeIdentity.same(string, other.string);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ChangeIdentity.hash(database), ChangeIdentity.hash(string));
    }
}
