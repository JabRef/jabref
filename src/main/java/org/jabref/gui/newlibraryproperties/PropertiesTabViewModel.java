package org.jabref.gui.newlibraryproperties;

public interface PropertiesTabViewModel {

    void setValues();

    void storeSettings();

    default boolean validateSettings() {
        return true;
    }
}
