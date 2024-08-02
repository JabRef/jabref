package org.jabref.gui.undo;

import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

public class CountingUndoManager extends UndoManager {

    private int unchangedPoint;
    private int current;

    @Override
    public synchronized boolean addEdit(UndoableEdit edit) {
        current++;
        boolean result = super.addEdit(edit);
        return result;
    }

    @Override
    public synchronized void undo() throws CannotUndoException {
        super.undo();
        current--;
    }

    @Override
    public synchronized void redo() throws CannotUndoException {
        super.redo();
        current++;
    }

    public synchronized void markUnchanged() {
        unchangedPoint = current;
    }

    public synchronized boolean hasChanged() {
        return current != unchangedPoint;
    }
}
