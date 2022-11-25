package org.jabref.logic.undo;

/**
 * Event sent when a new undoable action is added to the undo manager
 */
public class AddUndoableActionEvent extends UndoChangeEvent {

    public AddUndoableActionEvent(boolean canUndo, String undoDescription, boolean canRedo, String redoDescription) {
        super(canUndo, undoDescription, canRedo, redoDescription);
    }
}
