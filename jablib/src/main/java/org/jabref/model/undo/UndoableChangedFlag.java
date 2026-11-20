package org.jabref.model.undo;

import java.util.Map;
import java.util.Objects;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.types.EntryType;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Records an entry's "changed since parsing" flag around an in-place edit, so that undoing the edit also restores
/// whether the entry's parsed serialization may be reused on save.
///
/// The mark is only taken back when the entry holds again what it held while unmarked: an edit made meanwhile, which
/// the undo of the in-place edit refuses to take back, must not disappear behind a reused serialization on save.
///
/// @param unmarkedContent what the entry held while unmarked; `null` when it was marked already, as a mark is always safe to restore
@NullMarked
public record UndoableChangedFlag(BibEntry entry, boolean before, boolean after, @Nullable Content unmarkedContent) implements BibChange {

    public record Content(EntryType type, Map<Field, String> fields, String comments) {
        static Content of(BibEntry entry) {
            return new Content(entry.getType(), Map.copyOf(entry.getFieldMap()), entry.getUserComments());
        }

        boolean matches(BibEntry entry) {
            return type.equals(entry.getType()) && fields.equals(entry.getFieldMap()) && comments.equals(entry.getUserComments());
        }
    }

    /// Marks the entry as changed, remembering its content as long as it was unmarked.
    public static UndoableChangedFlag marking(BibEntry entry) {
        boolean before = entry.hasChanged();
        return new UndoableChangedFlag(entry, before, true, before ? null : Content.of(entry));
    }

    @Override
    public UndoableChangedFlag inverted() {
        return new UndoableChangedFlag(entry, after, before, unmarkedContent);
    }

    @Override
    public ApplyResult apply() {
        if (entry.hasChanged() != before) {
            return ApplyResult.of(this, "entry is " + (entry.hasChanged() ? "" : "not ") + "marked changed, unlike recorded");
        }
        if (!after && unmarkedContent != null && !unmarkedContent.matches(entry)) {
            return ApplyResult.of(this, "entry no longer holds what it held while unmarked");
        }
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
