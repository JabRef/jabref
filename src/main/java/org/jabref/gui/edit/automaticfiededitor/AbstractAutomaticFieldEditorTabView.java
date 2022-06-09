package org.jabref.gui.edit.automaticfiededitor;

import javafx.scene.layout.Pane;

public abstract class AbstractAutomaticFieldEditorTabView extends Pane implements AutomaticFieldEditorTab {

    @Override
    public Pane getContent() {
        return this;
    }
}
