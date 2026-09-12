package org.jabref.model.undo;

import java.util.Objects;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Records an entry's "changed since parsing" flag around an in-place edit, so that undoing the edit also restores
/// whether the entry's parsed serialization may be reused on save.
@NullMarked
public record UndoableChangedFlag(BibEntry entry, boolean before, boolean after) implements BibChange {
    @Override
    public UndoableChangedFlag inverted() {
        return new UndoableChangedFlag(entry, after, before);
    }

    @Override
    public ApplyResult apply() {
        entry.setChanged(after);
        return ApplyResult.SUCCESS;
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof UndoableChangedFlag other)
                && ChangeIdentity.same(entry, other.entry)
                && before == other.before
                && after == other.after;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ChangeIdentity.hash(entry), before, after);
    }
}
