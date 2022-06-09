package org.jabref.gui.edit.automaticfiededitor;

import javafx.scene.layout.AnchorPane;

public abstract class AbstractAutomaticFieldEditorTabView extends AnchorPane implements AutomaticFieldEditorTab {

    @Override
    public AnchorPane getContent() {
        return this;
    }
}
