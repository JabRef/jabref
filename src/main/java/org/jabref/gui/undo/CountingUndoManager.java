package org.jabref.gui.undo;

import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.undo.AddUndoableActionEvent;
import org.jabref.logic.undo.UndoRedoEvent;

import com.google.common.eventbus.EventBus;

public class CountingUndoManager extends UndoManager {

    private int unchangedPoint;
    private int current;

    private final EventBus eventBus = new EventBus();

    @Override
    public synchronized boolean addEdit(UndoableEdit edit) {
        current++;
        boolean returnvalue = super.addEdit(edit);
        postAddUndoEvent();
        return returnvalue;
    }

    @Override
    public synchronized void undo() throws CannotUndoException {
        super.undo();
        current--;
        postUndoRedoEvent();
    }

    @Override
    public synchronized void redo() throws CannotUndoException {
        super.redo();
        current++;
        postUndoRedoEvent();
    }

    public synchronized void markUnchanged() {
        unchangedPoint = current;
    }

    public synchronized boolean hasChanged() {
        return (current != unchangedPoint);
    }

    public void registerListener(Object object) {
        this.eventBus.register(object);
        postUndoRedoEvent(); // Send event to trigger changes
    }

    public void unregisterListener(Object object) {
        this.eventBus.unregister(object);
    }

    public void postUndoRedoEvent() {
        boolean canRedo = this.canRedo();
        boolean canUndo = this.canUndo();
        eventBus.post(new UndoRedoEvent(canUndo, canUndo ? getUndoPresentationName() : Localization.lang("Undo"),
                canRedo, canRedo ? getRedoPresentationName() : Localization.lang("Redo")));
    }

    private void postAddUndoEvent() {
        boolean canRedo = this.canRedo();
        boolean canUndo = this.canUndo();
        eventBus.post(new AddUndoableActionEvent(canUndo, canUndo ? getUndoPresentationName() : Localization.lang("Undo"),
                canRedo, canRedo ? getRedoPresentationName() : Localization.lang("Redo")));
    }
}
