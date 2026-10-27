/// The undo journal: the stack of changes a user can take back, and the API for putting changes on
/// it.
///
/// What a change *is* lives in [org.jabref.model.undo] — records describing one modification, from
/// which their own inverse is derived. This package is what turns a stream of such changes into a
/// history:
///
/// - [org.jabref.logic.undo.UndoManager] is the recording half, and all that the ~120 classes
///   editing a library need: `applyEdit(change)` performs and records in one operation,
///   `addEdit(name, block)` records whatever a block reports as one step, `markChanged()` reports a
///   write nobody recorded, and `suspendUndo(name)` holds the library while a command writes
///   changes it has not handed over yet.
/// - [org.jabref.logic.undo.JabRefUndoManager] adds what the few classes driving the undo UI need:
///   `undo()`, `redo()`, the saved position, and listener registration. It is plain Java — nothing
///   here touches JavaFX, so recording works in a unit test — and the JavaFX properties the menus
///   bind to live in `org.jabref.gui.undo`.
///
/// Two decisions shape what a *step* is, and both are the caller's to make rather than the
/// journal's to guess: a command records inside one block, and a text editor says that its change
/// is one keystroke of a run ([org.jabref.logic.undo.EditSource#TYPING]) so that a run becomes one
/// step, ending it with `endStep()` when it moves on. Everything else is a step of its own.
///
/// @see <a href="https://devdocs.jabref.org/decisions/0070-split-the-undo-manager-into-recording-and-gui-layers.html">ADR-0070</a>

package org.jabref.logic.undo;
