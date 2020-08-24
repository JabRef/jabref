package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;

import org.jabref.preferences.JabRefPreferences;

public class ImportTabViewModel implements PreferenceTabViewModel {

    private final JabRefPreferences preferences;

    public ImportTabViewModel(JabRefPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public void setValues() {

    }

    @Override
    public void storeSettings() {

    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public List<String> getRestartWarnings() {
        return new ArrayList<>();
    }
}
