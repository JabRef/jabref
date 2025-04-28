package org.jabref.gui.libraryproperties;

import javafx.scene.Node;

public interface PropertiesTab {
    Node getBuilder();

    String getTabName();

    void setValues();

    void storeSettings();

    boolean validateSettings();
}
