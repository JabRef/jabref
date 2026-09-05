---
parent: Requirements
---
# Undo and Redo

## The saved state of a library is identified, not counted
`req~logic.undo.saved-position-identity~1`

A library counts as unmodified exactly when its history stands at the position it was saved at.
Undoing back to that position reports the library unmodified again.
Reaching an equal number of applied changes along a different history does not: recording a change discards the redo stack, so after saving, undoing that change and editing again, the saved position no longer exists and can never be matched.
A position dropped because the stack reached its depth limit is the opposite case: the change stays applied to the library, so undoing everything that remains puts the library back at that position, and it counts as unmodified again if that is where it was saved.
This decides whether the modified marker is shown and whether closing the library offers to save it, so a wrong answer loses the user's work silently.

Needs: impl, utest

## Every library has its own undo history
`req~logic.undo.journal-per-library~1`

Each open library keeps its own undo history, and undo and redo act on the library the user is working in.
A change recorded against one library is never undone by undoing in another, and reaching the end of one library's history does not start undoing another's.
Saving a library sets its own saved position only, so a second library holding unsaved changes still reports itself modified and still offers to save when it is closed.
Closing a library discards its history, together with the entries the recorded changes refer to; reopening the library starts with an empty history.
The history is identified by the library, not by which library is in front, so a change made by a task that finishes after the user has switched libraries is still recorded against the library it was made in.

Needs: impl, utest

## An undoable change is applied and recorded as one operation
`req~logic.undo.apply-and-record-atomically~1`

When the undo journal performs a change, the change becomes visible in the library and present on the undo stack as a single operation.
No other thread can observe the library holding a change the journal does not yet know about.
An undo arriving from another thread therefore reverses the change it was aimed at, never the one before it, and the recorded history always describes a state the library actually had.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
