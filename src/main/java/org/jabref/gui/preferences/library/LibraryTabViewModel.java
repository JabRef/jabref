package org.jabref.gui.preferences.library;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.preferences.GeneralPreferences;
import org.jabref.preferences.TelemetryPreferences;

public class LibraryTabViewModel implements PreferenceTabViewModel {

    private final DialogService dialogService;
    private final GeneralPreferences generalPreferences;


    @SuppressWarnings("ReturnValueIgnored")
    public LibraryTabViewModel(DialogService dialogService, GeneralPreferences generalPreferences, TelemetryPreferences telemetryPreferences) {
        this.dialogService = dialogService;
        this.generalPreferences = generalPreferences;
    }

    public void setValues() {
    }

    public void storeSettings() {
        
    }
}
