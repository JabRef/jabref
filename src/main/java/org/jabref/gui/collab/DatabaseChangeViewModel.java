package org.jabref.gui.collab;

import javafx.scene.Node;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabaseContext;

abstract class DatabaseChangeViewModel {

    protected String name;
    private boolean accepted = true;

    DatabaseChangeViewModel() {
        name = "";
    }

    DatabaseChangeViewModel(String name) {
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
     * This method returns a {@link Node} detailing the nature and look of the change, e.g. how it is displayed
     * @return Node the Node with the layout of the change
     */
    public abstract Node description();

    /**
     * Performs the change. This method is responsible for adding a proper undo edit to
     * the NamedCompound, so the change can be undone.
     * @param database the database that should be modified accordingly.
     * @param undoEdit NamedCompound The compound to hold the undo edits.
     */
    public abstract void makeChange(BibDatabaseContext database, NamedCompound undoEdit);

}
