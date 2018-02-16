package org.jabref.logic.undo;

/**
 * Event sent when something is undone or redone
 *
 */
public class UndoRedoEvent extends UndoChangeEvent {

    public UndoRedoEvent(boolean canUndo, String undoDescription, boolean canRedo, String redoDescription) {
        super(canUndo, undoDescription, canRedo, redoDescription);
    }

}
