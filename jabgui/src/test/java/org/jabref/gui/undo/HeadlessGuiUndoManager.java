package org.jabref.gui.undo;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;

import org.jabref.logic.undo.JabRefUndoManager;

import org.jspecify.annotations.NullMarked;

/// A [GuiUndoManager] with a real journal and no JavaFX thread.
///
/// For tests that need something typed as [GuiUndoManager] — because that is what
/// [org.jabref.gui.LibraryTab#getUndoManager] hands out — while testing logic that only records
/// changes. [JabRefGuiUndoManager] would refresh its properties through the JavaFX thread on
/// every push, which without a started toolkit throws inside the listener; the journal catches
/// that and logs it, so the test passes while filling the log with stack traces.
///
/// The properties do track the stacks, so a test that reads them sees the truth rather than a
/// constant `false`. They are written on whichever thread recorded the change, which is exactly
/// what [JabRefGuiUndoManager] must not do and what makes this class unfit for anything but a
/// single-threaded test. The marshalling it skips is covered by [JabRefGuiUndoManagerTest],
/// which starts the toolkit on purpose.
@NullMarked
public class HeadlessGuiUndoManager extends JabRefUndoManager implements GuiUndoManager {

    private final ReadOnlyBooleanWrapper undoable = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper redoable = new ReadOnlyBooleanWrapper(false);

    public HeadlessGuiUndoManager() {
        // Straight from the notification, with no hop: a JavaFX property needs no toolkit, only
        // Platform.runLater does.
        addListener(() -> {
            undoable.set(canUndo());
            redoable.set(canRedo());
        });
    }

    @Override
    public ReadOnlyBooleanProperty undoableProperty() {
        return undoable.getReadOnlyProperty();
    }

    @Override
    public ReadOnlyBooleanProperty redoableProperty() {
        return redoable.getReadOnlyProperty();
    }
}
