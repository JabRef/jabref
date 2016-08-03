package net.sf.jabref.logic.undo;

/**
 * Event sent when a new undoable action is added to the undo manager
 *
 */
public class AddUndoableActionEvent {

    private final boolean canUndo;
    private final String undoDescription;
    private final boolean canRedo;
    private final String redoDescription;


    public AddUndoableActionEvent(boolean canUndo, String undoDescription, boolean canRedo, String redoDescription) {
        this.canUndo = canUndo;
        this.undoDescription = undoDescription;
        this.canRedo = canRedo;
        this.redoDescription = redoDescription;
    }

    /**
     *
     * @return true if there is an action that can be undone
     */
    public boolean isCanUndo() {
        return canUndo;
    }


    /**
    *
    * @return A description of the action to be undone
    */
    public String getUndoDescription() {
        return undoDescription;
    }


    /**
     *
     * @return true if there is an action that can be redone
     */
    public boolean isCanRedo() {
        return canRedo;
    }


    /**
    *
    * @return A description of the action to be redone
    */
    public String getRedoDescription() {
        return redoDescription;
    }

}
