package org.jabref.model.change;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibtexString;

import org.jspecify.annotations.NullMarked;

/// Insertion of a BibTeX string definition.
@NullMarked
public record StringInserted(BibtexString string) implements BibChange {

    @Override
    public StringRemoved inverted() {
        return new StringRemoved(string);
    }

    @Override
    public void applyTo(BibDatabaseContext context) {
        context.getDatabase().addString(string);
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof StringInserted other) && ChangeIdentity.same(string, other.string);
    }

    @Override
    public int hashCode() {
        return ChangeIdentity.hash(string);
    }
}
