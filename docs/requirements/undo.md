---
parent: Requirements
---
# Undo and Redo

## The saved state of a library is identified, not counted
`req~logic.undo.saved-position-identity~1`

A library counts as unmodified exactly when its history stands at the position it was saved at, and not when it has merely travelled the same distance along a different history.
This decides whether the modified marker is shown and whether closing the library offers to save it, so a wrong answer loses the user's work silently.

Needs: impl, utest

## Every library has its own undo history
`req~logic.undo.journal-per-library~1`

Each open library keeps its own undo history, and undo, redo and the saved position act on that library alone.
A change belongs to the library it was made in, whichever library is in front when it is recorded, and closing a library discards its history.

Needs: impl, utest

## An undoable change is applied and recorded as one operation
`req~logic.undo.apply-and-record-atomically~1`

When the undo journal performs a change, the change becomes visible in the library and present on the undo stack as a single operation.
No other thread can observe the library holding a change the journal does not yet know about, so an undo arriving from one reverses the change it was aimed at rather than the one before it.

Needs: impl, utest

## A command's writes are reserved against undo
`req~logic.undo.writes-reserved-against-undo~1`

While a command is applying changes it has not yet handed to the journal, undo and redo decline for that library and say which command holds it.
Taking a change back over writes that are not yet recorded would leave the library in a state no step on the stack describes, and the push that follows would discard the undone change.

Needs: impl, utest

## A change that the library has moved on from is refused, not applied
`req~logic.undo.stale-change-refused~1`

A change describing one value applies only while the library still holds the value it recorded, and reports the mismatch instead of writing.
A command writing on a background thread can have moved that value on since, and overwriting it would replace a newer value with an older one behind the user's back.

Needs: impl, utest

## Every group operation is one undo step
`req~logic.undo.group-operations-recorded~1`

Adding, removing, moving, sorting and editing groups each go on the undo stack as a single step, together with the entry assignments the operation changed.
Undoing one restores the tree that was there before it, and the assignments with it.

Needs: impl, utest

## The modified marker follows the journal
`req~logic.undo.modified-marker-derived~1`

A library counts as modified exactly when its journal stands away from the saved position, or when it was changed by something the journal could not record.
The marker is derived from that rather than set by each command, so no undo path has to remember to correct it afterwards.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
