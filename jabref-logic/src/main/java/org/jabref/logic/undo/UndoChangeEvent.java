package org.jabref.logic.undo;

/**
 * Event sent when something is undone or redone
 *
 */
public class UndoChangeEvent {

    private final boolean canUndo;
    private final String undoDescription;
    private final boolean canRedo;
    private final String redoDescription;


    public UndoChangeEvent(boolean canUndo, String undoDescription, boolean canRedo, String redoDescription) {
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
