package org.jabref.model.change;

import org.jabref.model.database.BibDatabaseContext;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// A change of the library preamble. A `null` value means no preamble is set.
@NullMarked
public record PreambleEdit(@Nullable String before, @Nullable String after) implements BibChange {

    @Override
    public PreambleEdit inverted() {
        return new PreambleEdit(after, before);
    }

    @Override
    public void applyTo(BibDatabaseContext context) {
        context.getDatabase().setPreamble(after);
    }
}
