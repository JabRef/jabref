package org.jabref.model.metadata.event;

/// Where a metadata change came from, travelling with [MetaDataChangedEvent] the way
/// [org.jabref.model.entry.event.EntriesEventSource] travels with an entry change.
///
/// A listener that derives the modified marker has to tell the two apart: a change the undo
/// journal recorded is described by a step, so undoing it brings the library back and the marker
/// has to clear again, while a change nobody recorded can only be taken back by saying so.
public enum MetaDataChangeSource {
    /// Nobody recorded this change. The default, so that a writer that says nothing is treated as
    /// unrecorded — the safe answer, since it costs an asterisk rather than the user's work.
    LOCAL,

    /// The undo journal is behind this change: a command recorded it, or it *is* an undo or a redo
    /// applying a step.
    JOURNAL
}
