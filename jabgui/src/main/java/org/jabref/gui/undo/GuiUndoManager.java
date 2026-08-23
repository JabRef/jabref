package org.jabref.gui.undo;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.undo.UndoManager;

import org.jspecify.annotations.NullMarked;

/// Exposes an [UndoManager]'s state as JavaFX properties, for binding menu and toolbar
/// enablement.
///
/// Deliberately separate from the manager, for the marshalling rather than the properties.
/// JavaFX properties themselves are just observable values and need no toolkit, but hopping to
/// the JavaFX thread does — and the manager used to do that on every push, so recording a
/// change from a plain unit test threw "Toolkit not initialized". Only an observer that feeds
/// the UI needs that hop, so it lives with the observer. The journal stays plain Java.
@NullMarked
public class GuiUndoManager {

    private final UndoManager undoManager;
    private final ReadOnlyBooleanWrapper undoable;
    private final ReadOnlyBooleanWrapper redoable;

    public GuiUndoManager(UndoManager undoManager) {
        this.undoManager = undoManager;
        this.undoable = new ReadOnlyBooleanWrapper(undoManager.canUndo());
        this.redoable = new ReadOnlyBooleanWrapper(undoManager.canRedo());

        undoManager.addListener(this::refresh);
    }

    public ReadOnlyBooleanProperty undoableProperty() {
        return undoable.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty redoableProperty() {
        return redoable.getReadOnlyProperty();
    }

    /// Reads the stacks first and sets the properties second, because the two steps may end up
    /// on different threads: the read has to happen where the notification arrived, while the
    /// write has to happen on the JavaFX thread.
    ///
    /// Applied inline when the edit was already made on the JavaFX thread, so a caller that
    /// records a change and then reads the property in the same event does not see the previous
    /// value. Deferring unconditionally would leave the menu stale for a pulse.
    private void refresh() {
        boolean canUndo = undoManager.canUndo();
        boolean canRedo = undoManager.canRedo();
        UiTaskExecutor.runNowOrInJavaFXThread(() -> {
            undoable.set(canUndo);
            redoable.set(canRedo);
        });
    }
}
