package org.jabref.model.change;

import java.util.Objects;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibtexString;

import org.jspecify.annotations.NullMarked;

/// Insertion of a BibTeX string definition.
@NullMarked
public record UndoableInsertString(BibDatabase database, BibtexString string) implements BibChange {

    @Override
    public UndoableRemoveString inverted() {
        return new UndoableRemoveString(database, string);
    }

    @Override
    public void apply() {
        database.addString(string);
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof UndoableInsertString other)
                && ChangeIdentity.same(database, other.database)
                && ChangeIdentity.same(string, other.string);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ChangeIdentity.hash(database), ChangeIdentity.hash(string));
    }
}
