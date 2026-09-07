package org.jabref.model.undo;

import org.jspecify.annotations.NullMarked;

/// A single reversible modification of a library.
///
/// Implementations are value objects: they hold the data needed to perform the change and the
/// data needed to undo it, and derive the undo direction from that rather than implementing it
/// separately. Undoing is therefore not a distinct operation — it is
/// `change.inverted().applyTo(context)`.
///
/// Implementations carry no user-facing text. A description exists only at the granularity a
/// user acts in, as the name of the enclosing [ChangeSet].
@NullMarked
public sealed interface BibChange permits
        ChangeSet,
        UndoableInsertEntries,
        UndoableRemoveEntries,
        UndoableChangeType,
        UndoableFieldChange,
        UndoableGroupChange,
        UndoableKeywordSeparatorChange,
        UndoableMetaDataChange,
        UndoableModifySubtree,
        UndoablePreambleChange,
        UndoableStringChange,
        UndoableInsertString,
        UndoableRemoveString {

    /// The change that reverses this one.
    ///
    /// Must be an involution: `change.inverted().inverted()` equals `change`.
    BibChange inverted();

    /// Performs this change.
    ///
    /// Implementations hold whatever they need to act on — an entry, a string, the database —
    /// so that recording a change never requires plumbing a context to the call site.
    ///
    /// The change is applied unconditionally rather than checking whether the library still
    /// holds the expected prior state; the undo stack is discarded whenever the library is
    /// reloaded, so that state is an invariant rather than something to verify.
    void apply();
}
