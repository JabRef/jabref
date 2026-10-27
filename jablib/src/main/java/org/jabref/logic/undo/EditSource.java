package org.jabref.logic.undo;

import org.jspecify.annotations.NullMarked;

/// Who is recording a change, as far as grouping into undo steps is concerned.
///
/// The journal groups a run of keystrokes into one step so that Ctrl+Z takes back a word rather
/// than a letter, and it cannot tell a keystroke from anything else: a field editor and a command
/// both record an [org.jabref.model.undo.UndoableFieldChange] against the same entry and field. So
/// the caller says which it is, and only typing is eligible to continue the step it lands on.
@NullMarked
public enum EditSource {
    /// One user action, recorded by whoever performed it. Never continues the step below it, and
    /// never invites the next change to continue it. The default.
    COMMAND,

    /// One keystroke of a run in a text editor, which the journal may fold into the step the
    /// previous keystroke produced — see [CoalescingPolicy].
    TYPING
}
