package org.jabref.gui.libraryproperties;

import org.jabref.model.metadata.MetaData;

public interface PropertiesTabViewModel {

    /// Fills this tab's controls from `metaData` — see [PropertiesTab#setValues(MetaData)].
    void setValues(MetaData metaData);

    /// Writes this tab's settings into `metaData` — see [PropertiesTab#storeSettings(MetaData)].
    /// A tab whose settings are not metadata, such as the preamble, ignores it and records its
    /// own change.
    void storeSettings(MetaData metaData);

    default boolean validateSettings() {
        return true;
    }
}
