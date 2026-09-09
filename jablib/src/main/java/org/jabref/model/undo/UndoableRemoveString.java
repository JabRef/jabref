package org.jabref.model.undo;

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
    public ApplyResult apply() {
        // By identity, not by name: a different string under the same name means the one this
        // change recorded is gone, and removing by its id would quietly do nothing at all.
        if (database.getStringByName(string.getName()).filter(present -> present == string).isEmpty()) {
            return ApplyResult.of(this, "the string '%s' this change recorded is no longer in the library".formatted(string.getName()));
        }
        database.removeString(string.getId());
        return ApplyResult.SUCCESS;
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
