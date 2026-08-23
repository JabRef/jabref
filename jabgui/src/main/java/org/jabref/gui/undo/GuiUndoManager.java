package org.jabref.gui.undo;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.model.undo.UndoManager;

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

    private void refresh() {
        boolean canUndo = undoManager.canUndo();
        boolean canRedo = undoManager.canRedo();
        UiTaskExecutor.runInJavaFXThread(() -> {
            undoable.set(canUndo);
            redoable.set(canRedo);
        });
    }
}
