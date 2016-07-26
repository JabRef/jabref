package net.sf.jabref.logic.undo;


public class AddUndoEvent {

    public final boolean canUndo;
    public final String undoText;
    public final boolean canRedo;
    public final String redoText;


    public AddUndoEvent(boolean canUndo, String undoText, boolean canRedo, String redoText) {
        this.canUndo = canUndo;
        this.undoText = undoText;
        this.canRedo = canRedo;
        this.redoText = redoText;
    }
}
