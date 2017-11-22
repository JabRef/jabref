package org.jabref.gui.fieldeditors;

import javafx.scene.Parent;

import org.jabref.model.entry.BibEntry;

public interface FieldEditorFX {

    void bindToEntry(BibEntry entry);

    Parent getNode();

    default void requestFocus() {
        getNode().requestFocus();
    }

    /**
     * Returns relative size of the field editor in terms of display space.
     *
     * A value of 1 means that the editor gets exactly as much space as all other regular editors.
     * A value of 2 means that the editor gets twice as much space as regular editors.
     *
     * @return the relative weight of the editor in terms of display space
     */
    default double getWeight() { return 1; }
}
