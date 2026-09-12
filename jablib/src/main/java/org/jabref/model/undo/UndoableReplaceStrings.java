package org.jabref.model.undo;

import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibtexString;

import org.jspecify.annotations.NullMarked;

/// Replaces a library's string constants as a whole, which is how the library properties dialog
/// writes them: the tab hands over the list it built, rather than the edits that produced it.
///
/// Not to be confused with [UndoableStringChange], which changes the name or the content of a
/// single constant and, being a single value, verifies it before writing. This one is a collection
/// and applies unconditionally; see [BibChange#apply].
@NullMarked
public record UndoableReplaceStrings(BibDatabase database, List<BibtexString> before, List<BibtexString> after) implements BibChange {

    public UndoableReplaceStrings {
        before = List.copyOf(before);
        after = List.copyOf(after);
    }

    @Override
    public UndoableReplaceStrings inverted() {
        return new UndoableReplaceStrings(database, after, before);
    }

    @Override
    public ApplyResult apply() {
        database.setStrings(after);
        return ApplyResult.SUCCESS;
    }
}
