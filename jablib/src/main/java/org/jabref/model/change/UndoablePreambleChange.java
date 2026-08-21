package org.jabref.model.change;

import java.util.Objects;

import org.jabref.model.database.BibDatabase;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// A change of the library preamble. A `null` value means no preamble is set.
@NullMarked
public record UndoablePreambleChange(BibDatabase database, @Nullable String before, @Nullable String after) implements BibChange {

    @Override
    public UndoablePreambleChange inverted() {
        return new UndoablePreambleChange(database, after, before);
    }

    @Override
    public void apply() {
        database.setPreamble(after);
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof UndoablePreambleChange other)
                && ChangeIdentity.same(database, other.database)
                && Objects.equals(before, other.before)
                && Objects.equals(after, other.after);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ChangeIdentity.hash(database), before, after);
    }
}
