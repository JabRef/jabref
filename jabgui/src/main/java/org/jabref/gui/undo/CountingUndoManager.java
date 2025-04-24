package org.jabref.gui.undo;

import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

import org.jabref.gui.util.UiTaskExecutor;

public class CountingUndoManager extends UndoManager {

    private int unchangedPoint;

    /**
     * Indicates the number of edits aka balance of edits on the stack +1 when an edit is added/redone and -1 when an edit is undoed.
     */
    private final IntegerProperty balanceProperty = new SimpleIntegerProperty(0);
    private final BooleanProperty undoableProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty redoableProperty = new SimpleBooleanProperty(false);

    @Override
    public synchronized boolean addEdit(UndoableEdit edit) {
        boolean editAdded = super.addEdit(edit);
        if (editAdded) {
            incrementBalance();
            updateUndoableStatus();
            updateRedoableStatus();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public synchronized void undo() throws CannotUndoException {
        super.undo();
        decrementBalance();
        updateUndoableStatus();
        updateRedoableStatus();
    }

    @Override
    public synchronized void redo() throws CannotUndoException {
        super.redo();
        incrementBalance();
        updateUndoableStatus();
        updateRedoableStatus();
    }

    public synchronized void markUnchanged() {
        unchangedPoint = balanceProperty.get();
    }

    public synchronized boolean hasChanged() {
        return balanceProperty.get() != unchangedPoint;
    }

    private void incrementBalance() {
        balanceProperty.setValue(balanceProperty.getValue() + 1);
    }

    private void decrementBalance() {
        balanceProperty.setValue(balanceProperty.getValue() - 1);
    }

    private void updateUndoableStatus() {
        UiTaskExecutor.runInJavaFXThread(() -> undoableProperty.setValue(canUndo()));
    }

    private void updateRedoableStatus() {
        UiTaskExecutor.runInJavaFXThread(() -> redoableProperty.setValue(canRedo()));
    }

    public ReadOnlyBooleanProperty getUndoableProperty() {
        return undoableProperty;
    }

    public ReadOnlyBooleanProperty getRedoableProperty() {
        return redoableProperty;
    }
}
