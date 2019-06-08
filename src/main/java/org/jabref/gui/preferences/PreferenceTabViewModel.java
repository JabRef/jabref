package org.jabref.gui.preferences;

public interface PreferenceTabViewModel {

    void setValues();

    void storeSettings();

    boolean validateSettings(); // ToDo: Remove this after implementation of MVVMFX Validator
}
