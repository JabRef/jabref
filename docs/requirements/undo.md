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

<!-- markdownlint-disable-file MD022 -->
