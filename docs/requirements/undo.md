---
parent: Requirements
---
# Undo and Redo

## The saved state of a library is identified, not counted
`req~logic.undo.saved-position-identity~1`

A library counts as unmodified exactly when its history stands at the position it was saved at.
Undoing back to that position reports the library unmodified again.
Reaching an equal number of applied changes along a different history does not: recording a change discards the redo stack, so after saving, undoing that change and editing again, the saved position no longer exists and can never be matched.
The same holds for a position dropped when the stack reaches its depth limit.
This decides whether the modified marker is shown and whether closing the library offers to save it, so a wrong answer loses the user's work silently.

Needs: impl, utest

## An undoable change is applied and recorded as one operation
`req~logic.undo.apply-and-record-atomically~1`

When the undo journal performs a change, the change becomes visible in the library and present on the undo stack as a single operation.
No other thread can observe the library holding a change the journal does not yet know about.
An undo arriving from another thread therefore reverses the change it was aimed at, never the one before it, and the recorded history always describes a state the library actually had.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
