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
    public ApplyResult apply() {
        String current = switch (part) {
            case NAME ->
                    string.getName();
            case CONTENT ->
                    string.getContent();
        };
        if (!Objects.equals(current, before)) {
            return ApplyResult.of(this, "the string's %s is '%s', not the recorded '%s'".formatted(part, current, before));
        }
        switch (part) {
            case NAME ->
                    string.setName(after);
            case CONTENT ->
                    string.setContent(after);
        }
        return ApplyResult.SUCCESS;
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
