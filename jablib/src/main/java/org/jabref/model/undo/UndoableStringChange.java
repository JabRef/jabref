package org.jabref.model.undo;

import java.util.Objects;

import org.jabref.model.entry.BibtexString;

import org.jspecify.annotations.NullMarked;

/// A change of a BibTeX string's name or content.
@NullMarked
public record UndoableStringChange(BibtexString string, Part part, String before, String after) implements BibChange {

    public enum Part { NAME, CONTENT }

    @Override
    public UndoableStringChange inverted() {
        return new UndoableStringChange(string, part, after, before);
    }

    @Override
    public void apply() {
        switch (part) {
            case NAME ->
                    string.setName(after);
            case CONTENT ->
                    string.setContent(after);
        }
    }

    @Override
    public boolean equals(Object object) {
        return (object instanceof UndoableStringChange other)
                && ChangeIdentity.same(string, other.string)
                && (part == other.part)
                && before.equals(other.before)
                && after.equals(other.after);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ChangeIdentity.hash(string), part, before, after);
    }
}
