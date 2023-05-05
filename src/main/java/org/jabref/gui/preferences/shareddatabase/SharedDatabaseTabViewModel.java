package org.jabref.gui.preferences.shareddatabase;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.preferences.ExternalApplicationsPreferences;

public class SharedDatabaseTabViewModel implements PreferenceTabViewModel {

    private final BooleanProperty connectLastStartupProperty = new SimpleBooleanProperty();
    private final ExternalApplicationsPreferences externalApplicationsPreferences;

    public SharedDatabaseTabViewModel(ExternalApplicationsPreferences externalApplicationsPreferences) {
        this.externalApplicationsPreferences = externalApplicationsPreferences;
    }

    @Override
    public void setValues() {
        connectLastStartupProperty.setValue(externalApplicationsPreferences.shouldAutoConnectToLastSharedDatabases());
    }

    @Override
    public void storeSettings() {
        externalApplicationsPreferences.setAutoConnectToLastSharedDatabases(connectLastStartupProperty.getValue());
    }

    public BooleanProperty connectLastStartupProperty() {
        return connectLastStartupProperty;
    }
}
