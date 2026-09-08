package org.jabref.model.undo;

import org.jspecify.annotations.NullMarked;

/// A single reversible modification of a library.
///
/// Implementations are value objects: they hold the data needed to perform the change and the
/// data needed to undo it, and derive the undo direction from that rather than implementing it
/// separately. Undoing is therefore not a distinct operation — it is `change.inverted().apply()`.
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
        UndoableGroupTreeChange,
        UndoableKeywordSeparatorChange,
        UndoableMetaDataChange,
        UndoablePreambleChange,
        UndoableStringChange,
        UndoableReplaceStrings,
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
    /// **A change that describes one value refuses when the library no longer holds that value**,
    /// because a command writing on a background thread can have moved it on since, and writing
    /// over it would produce a library no step on the stack describes. A change that describes a
    /// collection or a whole subtree applies unconditionally: comparing all of it on every apply
    /// costs more than the case is worth, and a partial comparison would only look like a check.
    ///
    /// A change describing one modification performs it or throws, and so always returns
    /// [ApplyResult#SUCCESS]. Only [ChangeSet] can apply part of what it describes, and the
    /// return value is how it says so instead of reporting a success it did not deliver.
    ///
    /// @return what was applied, and what was not
    ApplyResult apply();
}
