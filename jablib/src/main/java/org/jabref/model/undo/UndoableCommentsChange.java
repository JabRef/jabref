package org.jabref.model.undo;

import java.util.Objects;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// A change of the comments written before an entry in the library file.
@NullMarked
public record UndoableCommentsChange(BibEntry entry, String before, String after) implements BibChange {
    @Override
    public UndoableCommentsChange inverted() {
        return new UndoableCommentsChange(entry, after, before);
    }

    @Override
    public ApplyResult apply() {
        if (!entry.getUserComments().equals(before)) {
            return ApplyResult.of(this, "entry comments are not the recorded ones");
        }
        entry.setCommentsBeforeEntry(after);
        // The setter is the parser's and leaves the flag alone; here the comment differs from what was parsed
        entry.setChanged(true);
        return ApplyResult.SUCCESS;
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof UndoableCommentsChange other)
                && ChangeIdentity.same(entry, other.entry)
                && before.equals(other.before)
                && after.equals(other.after);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ChangeIdentity.hash(entry), before, after);
    }
}
