package org.jabref.logic.undo;

import java.util.function.Consumer;

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
    void applyEdit(BibChange change);
}
