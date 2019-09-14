package org.jabref.gui.preferences;

import org.jabref.gui.DialogService;
import org.jabref.preferences.JabRefPreferences;

import java.util.ArrayList;
import java.util.List;

public class TableTabViewModel implements PreferenceTabViewModel {

    private final DialogService dialogService;
    private final JabRefPreferences preferences;

    public TableTabViewModel(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
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
