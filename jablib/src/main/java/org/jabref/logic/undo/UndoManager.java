package org.jabref.logic.undo;

import java.util.function.Consumer;

import org.jabref.model.undo.ApplyResult;
import org.jabref.model.undo.BibChange;
import org.jabref.model.undo.CompoundEdit;

import org.jspecify.annotations.NullMarked;

/// Puts changes on the undo journal. What almost every client of undo actually needs.
///
/// Undoing, redoing, asking whether the library differs from the last saved position and
/// subscribing to stack changes are the business of a handful of classes — the Undo and Redo
/// actions, the menu bindings and the library tab that draws the modified marker. Everything
/// else edits the library and hands the change over, and there are roughly 120 such classes.
/// Passing them the whole manager hands every field editor, cleanup and import task the ability
/// to rewrite the user's history, when all any of them does is describe what it just changed.
///
/// Those classes depend on this type; the few that drive the stacks depend on
/// [JabRefUndoManager]. What a class asks for therefore says what it does with it.
@NullMarked
public interface UndoManager {

    /// Records a change the caller has already made.
    void addEdit(BibChange change);

    /// Runs `mutations` and records whatever it reports as one undo step named `name`.
    ///
    /// @return whether anything was recorded, for callers that report the outcome to the user
    boolean addEdit(String name, Consumer<CompoundEdit> mutations);

    /// Performs `change` and records it in one go.
    ///
    /// @return what was applied, and what was not — see [BibChange#apply]
    ApplyResult applyEdit(BibChange change);

    /// Marks the library as changed by something this journal cannot take back — a migration on
    /// load, an external change the user denied, a setting written without being recorded.
    ///
    /// The modified marker derives from the saved position, so a write nobody recorded has to say
    /// so here, or the library looks saved. Saving is what clears it again.
    void markChanged();

    /// Suspends undo and redo for this library while the caller applies changes it has not yet
    /// handed over. The undo UI reads the other end of this through `GuiUndoManager#suspendedBy`.
    ///
    /// A command that mutates on a background thread writes to the library long before anything
    /// reaches the stack, and an undo arriving in that window takes back a change *underneath*
    /// those writes: the library then holds a state no step on the stack describes, and the push
    /// that follows discards the undone change with the redo stack. Suspending is how a caller says
    /// that window is open.
    ///
    /// Nothing waits: undo and redo decline while a suspension is open rather than blocking on it,
    /// so a long import can never freeze the JavaFX thread on Ctrl+Z.
    ///
    /// [#addEdit(String,Consumer)] suspends for the duration of the block, so only the commands
    /// that collect by hand need this. Close it **after** the push, or the window reopens between
    /// the last write and the record.
    ///
    /// @param name the command holding the library, as the user would recognise it — shown when
    ///             undo declines
    UndoSuspension suspendUndo(String name);
}
