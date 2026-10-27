package org.jabref.gui.edit.automaticfieldeditor;

import javafx.scene.layout.Pane;

public interface AutomaticFieldEditorTab {
    Pane getContent();

    String getTabName();
}
