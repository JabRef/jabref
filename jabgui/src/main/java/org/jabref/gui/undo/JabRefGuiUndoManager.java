package org.jabref.gui.undo;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.undo.JabRefUndoManager;

import org.jspecify.annotations.NullMarked;

/// The journal, plus the JavaFX properties the menus bind to.
///
/// The properties live in jabgui, and the marshalling is the reason: JavaFX properties are just
/// observable values and need no toolkit, but hopping to the JavaFX thread does — and the
/// Swing-era manager did that on every push, so recording a change from a plain unit test threw
/// "Toolkit not initialized". Only an observer that feeds the UI needs the hop, so it lives with
/// the observer while [JabRefUndoManager] stays plain Java.
///
/// Extends rather than wraps, following `JabRefGuiPreferences extends JabRefCliPreferences`.
/// Wrapping meant every caller reached through a `getUndoManager()` accessor to do anything, and
/// meant two objects where the application only ever has one.
@NullMarked
public class JabRefGuiUndoManager extends JabRefUndoManager implements GuiUndoManager {

    private final ReadOnlyBooleanWrapper undoable = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper redoable = new ReadOnlyBooleanWrapper(false);

    public JabRefGuiUndoManager() {
        // Subscribing to itself rather than refreshing inside the push: listeners are notified
        // after the monitor is released, which is exactly where a hop to the JavaFX thread has
        // to happen. Doing it any earlier would hold the journal's lock across a wait for
        // another thread.
        addListener(this::refresh);
    }

    @Override
    public ReadOnlyBooleanProperty undoableProperty() {
        return undoable.getReadOnlyProperty();
    }

    @Override
    public ReadOnlyBooleanProperty redoableProperty() {
        return redoable.getReadOnlyProperty();
    }

    /// Reads the stacks on the JavaFX thread rather than where the notification arrived, so that
    /// what is written is what the journal holds at the moment of writing. Reading first and
    /// carrying the values over would let a thread that read an older state post after one that
    /// read a newer state, leaving the menu enabled over an empty stack until the next edit.
    ///
    /// Applied inline when the edit was already made on the JavaFX thread, so a caller that
    /// records a change and then reads the property in the same event does not see the previous
    /// value. Deferring unconditionally would leave the menu stale for a pulse.
    ///
    /// A burst of edits therefore queues one update per edit, and they are not coalesced: each
    /// reads the current state, so every update after the first sets the value already there,
    /// which a JavaFX property ignores without notifying anything.
    private void refresh() {
        UiTaskExecutor.runNowOrInJavaFXThread(() -> {
            undoable.set(canUndo());
            redoable.set(canRedo());
        });
    }
}
