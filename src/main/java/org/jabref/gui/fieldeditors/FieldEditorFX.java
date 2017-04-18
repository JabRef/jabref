package org.jabref.gui.fieldeditors;

import javafx.scene.Parent;

import org.jabref.model.entry.BibEntry;

public interface FieldEditorFX {

    void bindToEntry(BibEntry entry);

    Parent getNode();

    default void requestFocus() {
        getNode().requestFocus();
    }
}
