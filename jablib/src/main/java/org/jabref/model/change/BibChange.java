package org.jabref.model.change;

import org.jabref.model.database.BibDatabaseContext;

/// A single reversible modification of a library.
///
/// Implementations are value objects: they hold the data needed to perform the change and the
/// data needed to undo it, and derive the undo direction from that rather than implementing it
/// separately. Undoing is therefore not a distinct operation — it is
/// `change.inverted().applyTo(context)`.
///
/// Implementations carry no user-facing text. A description exists only at the granularity a
/// user acts in, as the name of the enclosing [ChangeSet].
public sealed interface BibChange
        permits ChangeSet, EntriesInserted, EntriesRemoved, EntryTypeEdit, FieldEdit, PreambleEdit, StringEdit, StringInserted, StringRemoved {

    /// The change that reverses this one.
    ///
    /// Must be an involution: `change.inverted().inverted()` equals `change`.
    BibChange inverted();

    /// Performs this change on `context`.
    ///
    /// Implementations apply their change unconditionally rather than checking whether the
    /// library still holds the expected prior state; the undo stack is discarded whenever the
    /// library is reloaded, so that state is an invariant rather than something to verify.
    void applyTo(BibDatabaseContext context);
}
