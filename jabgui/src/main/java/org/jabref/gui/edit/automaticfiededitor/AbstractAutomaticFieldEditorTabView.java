package org.jabref.gui.edit.automaticfiededitor;

import javafx.scene.layout.VBox;

public abstract class AbstractAutomaticFieldEditorTabView extends VBox implements AutomaticFieldEditorTab {

    @Override
    public VBox getContent() {
        return this;
    }
}
