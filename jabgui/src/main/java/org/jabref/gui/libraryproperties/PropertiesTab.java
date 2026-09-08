package org.jabref.gui.libraryproperties;

import javafx.scene.Node;

import org.jabref.model.metadata.MetaData;

public interface PropertiesTab {
    Node getBuilder();

    String getTabName();

    void setValues();

    /// Writes this tab's settings into `metaData`.
    ///
    /// @param metaData the library's settings to write to — a working copy the dialog installs
    ///                 afterwards, so that the whole dialog reaches the library as one recorded
    ///                 change rather than as one event per setting
    void storeSettings(MetaData metaData);

    boolean validateSettings();
}
