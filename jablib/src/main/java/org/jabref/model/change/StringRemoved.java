package org.jabref.model.change;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibtexString;

import org.jspecify.annotations.NullMarked;

/// Removal of a BibTeX string definition.
@NullMarked
public record StringRemoved(BibtexString string) implements BibChange {

    @Override
    public StringInserted inverted() {
        return new StringInserted(string);
    }

    @Override
    public void applyTo(BibDatabaseContext context) {
        context.getDatabase().removeString(string.getId());
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof StringRemoved other) && ChangeIdentity.same(string, other.string);
    }

    @Override
    public int hashCode() {
        return ChangeIdentity.hash(string);
    }
}
