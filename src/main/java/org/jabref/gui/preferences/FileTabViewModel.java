package org.jabref.gui.preferences;

import org.jabref.gui.DialogService;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.NewLineSeperator;

public class FileTabViewModel implements PreferenceTabViewModel {

    private final DialogService dialogService;
    private final JabRefPreferences preferences;

    public FileTabViewModel(DialogService dialogService, JabRefPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        setValues();
    }

    public void setValues() {

    }

    public void storeSettings() {

    }

    public boolean validateSettings() {
         return true;
    }

    public void mainFileDirBrowse () {
        // ToDo
    }
}

