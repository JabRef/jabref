package org.jabref.gui.edit.automaticfiededitor;

import javafx.scene.layout.Pane;

public interface AutomaticFieldEditorTab {
    Pane getContent();

    String getTabName();

    void registerListener(Object object);

    void unregisterListener(Object object);
}
