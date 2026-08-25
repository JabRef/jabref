package org.jabref.gui.undo;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.undo.JabRefUndoManager;

import org.jspecify.annotations.NullMarked;

/// Exposes an [JabRefUndoManager]'s state as JavaFX properties, for binding menu and toolbar
/// enablement.
///
/// Deliberately separate from the manager, for the marshalling rather than the properties.
/// JavaFX properties themselves are just observable values and need no toolkit, but hopping to
/// the JavaFX thread does — and the manager used to do that on every push, so recording a
/// change from a plain unit test threw "Toolkit not initialized". Only an observer that feeds
/// the UI needs that hop, so it lives with the observer. The journal stays plain Java.
@NullMarked
public class GuiUndoManager {

    private final JabRefUndoManager undoManager;
    private final ReadOnlyBooleanWrapper undoable;
    private final ReadOnlyBooleanWrapper redoable;

    public GuiUndoManager(JabRefUndoManager undoManager) {
        this.undoManager = undoManager;
        this.undoable = new ReadOnlyBooleanWrapper(undoManager.canUndo());
        this.redoable = new ReadOnlyBooleanWrapper(undoManager.canRedo());

        undoManager.addListener(this::refresh);
    }

    public JabRefUndoManager getUndoManager() {
        return undoManager;
    }

    public ReadOnlyBooleanProperty undoableProperty() {
        return undoable.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty redoableProperty() {
        return redoable.getReadOnlyProperty();
    }

    /// Reads the stacks on the JavaFX thread rather than where the notification arrived, so that
    /// what is written is what the manager holds at the moment of writing. Reading first and
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
            undoable.set(undoManager.canUndo());
            redoable.set(undoManager.canRedo());
        });
    }
}
