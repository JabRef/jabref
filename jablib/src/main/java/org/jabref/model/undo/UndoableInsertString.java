package org.jabref.model.undo;

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
    public ApplyResult apply() {
        if (database.hasStringByName(string.getName())) {
            return ApplyResult.of(this, "a string named '%s' is already in the library".formatted(string.getName()));
        }
        database.addString(string);
        return ApplyResult.SUCCESS;
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
