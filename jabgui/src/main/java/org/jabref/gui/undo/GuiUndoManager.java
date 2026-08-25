package org.jabref.gui.undo;

import javafx.beans.property.ReadOnlyBooleanProperty;

import org.jabref.logic.undo.UndoManager;

import org.jspecify.annotations.NullMarked;

/// The undo journal as the GUI needs it: the stacks can be driven, and their state can be
/// observed as JavaFX properties for menu and toolbar enablement.
///
/// Layered the way JabRef layers preferences — [org.jabref.logic.preferences.CliPreferences] to
/// `GuiPreferences`, `JabRefCliPreferences` to `JabRefGuiPreferences`. [UndoManager] in jablib
/// is the recording half that the ~118 classes editing the library depend on; this adds what
/// only the undo UI needs, so a class's declared type still says what it does with the journal.
///
/// The control methods are declared here rather than inherited because an interface cannot
/// inherit from a class: [org.jabref.logic.undo.JabRefUndoManager] already implements every one
/// of them, and [JabRefGuiUndoManager] brings the two together.
@NullMarked
public interface GuiUndoManager extends UndoManager {

    /// Reverses the change on top of the undo stack.
    void undo();

    /// Re-applies the change last undone.
    void redo();

    boolean canUndo();

    boolean canRedo();

    /// Whether the library differs from the last saved position.
    boolean hasChanged();

    /// Marks the current position as saved.
    void markUnchanged();

    /// Discards both stacks and the saved position.
    ///
    /// Nothing calls this today, which is a defect rather than a spare method: closing a library
    /// leaves its changes on the stack, so a later undo re-applies them against a database that
    /// is gone, and the entries they hold stay alive for the session. Calling it on close needs
    /// the journal to belong to a library first — one journal currently serves them all.
    void clear();

    /// Notified after every change to either stack, from whichever thread made it.
    void addListener(Runnable listener);

    /// Whether there is anything to undo, for binding menu and toolbar enablement.
    ReadOnlyBooleanProperty undoableProperty();

    /// Whether there is anything to redo.
    ReadOnlyBooleanProperty redoableProperty();
}
