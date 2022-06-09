package org.jabref.gui.edit.automaticfiededitor;

import javafx.scene.layout.Region;

public abstract class AbstractAutomaticFieldEditorTabView extends Region implements AutomaticFieldEditorTab {

    @Override
    public Region getContent() {
        return this;
    }
}
