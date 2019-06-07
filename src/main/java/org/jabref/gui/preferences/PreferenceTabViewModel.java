package org.jabref.gui.preferences;

public interface PreferenceTabViewModel {

    void setValues();

    void storeSettings();

    boolean validateSettings();

}
