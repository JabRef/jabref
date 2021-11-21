package org.jabref.gui.newlibraryproperties;

import javafx.scene.Node;

public interface PropertiesTab {
    Node getBuilder();

    String getTabName();

    void setValues();

    void storeSettings();

    boolean validateSettings();
}
