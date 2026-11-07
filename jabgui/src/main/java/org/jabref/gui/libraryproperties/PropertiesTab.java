package org.jabref.gui.libraryproperties;

import javafx.scene.Node;

import org.jabref.model.metadata.MetaData;

public interface PropertiesTab {
    Node getBuilder();

    String getTabName();

    /// Fills this tab's controls from `metaData`.
    ///
    /// @param metaData the library's settings, as they stand
    void setValues(MetaData metaData);

    /// Writes this tab's settings into `metaData`.
    ///
    /// @param metaData the settings to write to — a working copy the dialog installs afterwards,
    ///                 so that the whole dialog reaches the library as one recorded change rather
    ///                 than as one event per setting. A tab reads and writes the instance it is
    ///                 handed and holds none of its own, or it would read one library's settings
    ///                 and write another's
    void storeSettings(MetaData metaData);

    boolean validateSettings();
}
