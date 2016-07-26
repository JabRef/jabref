package net.sf.jabref.logic.undo;


public class UndoRedoEvent extends AddUndoEvent {

    public UndoRedoEvent(boolean canUndo, String undoText, boolean canRedo, String redoText) {
        super(canUndo, undoText, canRedo, redoText);
    }

}
