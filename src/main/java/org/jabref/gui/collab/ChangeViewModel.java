package org.jabref.gui.collab;

import javax.swing.JComponent;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jabref.gui.BasePanel;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabase;

abstract class ChangeViewModel extends DefaultMutableTreeNode {

    protected String name;
    private boolean accepted = true;


    ChangeViewModel() {
        name = "";
    }

    ChangeViewModel(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean a) {
        accepted = a;
    }

    /**
     * This method is used to disable the "accept" box if the parent has been set to "not accepted".
     * Thus the user can disable e.g. an entry change without having to disable all field changes.
     * @return boolean false if the parent overrides by not being accepted.
     */
    public boolean isAcceptable() {
        if ((getParent() != null) && (getParent() instanceof ChangeViewModel)) {
            return ((ChangeViewModel) getParent()).isAccepted();
        } else {
            return true;
        }
    }

    /**
     * This method returns a JComponent detailing the nature of the change.
     * @return JComponent
     */
    public abstract JComponent description();

    /**
     * Perform the change. This method is responsible for adding a proper undo edit to
     * the NamedCompound, so the change can be undone.
     * @param panel BasePanel The tab where the database lives.
     * @param secondary BibDatabase The "tmp" database for which the change
     *   should also be made.
     * @param undoEdit NamedCompound The compound to hold the undo edits.
     * @return true if all changes were made, false if not all were accepted.
     */
    public abstract boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit);

}
