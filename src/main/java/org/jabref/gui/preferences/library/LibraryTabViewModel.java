package org.jabref.gui.preferences.library;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.preferences.GeneralPreferences;
import org.jabref.preferences.TelemetryPreferences;

public class LibraryTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty collectTelemetryProperty = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final GeneralPreferences generalPreferences;
    private final TelemetryPreferences telemetryPreferences;

    @SuppressWarnings("ReturnValueIgnored")
    public LibraryTabViewModel(DialogService dialogService, GeneralPreferences generalPreferences, TelemetryPreferences telemetryPreferences) {
        this.dialogService = dialogService;
        this.generalPreferences = generalPreferences;
        this.telemetryPreferences = telemetryPreferences;
    }

    public void setValues() {
        collectTelemetryProperty.setValue(telemetryPreferences.shouldCollectTelemetry());
    }

    public void storeSettings() {
        telemetryPreferences.setCollectTelemetry(collectTelemetryProperty.getValue());
    }

    public BooleanProperty collectTelemetryProperty() {
        return this.collectTelemetryProperty;
    }
}
