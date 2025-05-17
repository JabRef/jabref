package org.jabref.gui.libraryproperties;

public interface PropertiesTabViewModel {

    void setValues();

    void storeSettings();

    default boolean validateSettings() {
        return true;
    }
}
