package org.jabref.gui.edit.automaticfiededitor;

import javafx.scene.layout.Pane;

public interface AutomaticFieldEditorTab {
    Pane getContent();

    String getTabName();
}
