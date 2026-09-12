package org.jabref.model;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// One field of one entry, modified, removed or added: what it held before and what it holds now.
///
/// Returned by everything in jablib that edits entries — cleanups, formatters, group assignment —
/// so that a caller can report or record what happened. [org.jabref.model.undo.UndoableFieldChange]
/// is the same four values as an undoable change, and is built from one of these.
///
/// Two changes are equal when they describe the same modification of equal entries — the entry is
/// compared as [BibEntry] compares. [org.jabref.model.undo.UndoableFieldChange] deliberately does
/// not: a step on the undo stack has to describe *that* entry object, so it compares the entry by
/// identity. That difference is why the two are not one type.
///
/// @param entry    the entry whose field changed
/// @param field    the field that changed
/// @param oldValue what the field held before, or `null` when it had no value
/// @param newValue what the field holds now, or `null` when it was removed
@NullMarked
public record FieldChange(BibEntry entry, Field field, @Nullable String oldValue, @Nullable String newValue) {
    @Override
    public String toString() {
        return "FieldChange [entry=" + entry.getCitationKey().orElse("") + ", field=" + field + ", oldValue="
                + oldValue + ", newValue=" + newValue + "]";
    }
}
