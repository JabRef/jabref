package org.jabref.gui.edit.automaticfiededitor;

import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

public interface AutomaticFieldEditorTab {
    Pane getContent();

    String getTabName();
}
