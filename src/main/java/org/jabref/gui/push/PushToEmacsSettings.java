package org.jabref.gui.push;

import javafx.beans.property.ObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.PushToApplicationPreferences;

public class PushToEmacsSettings extends PushToApplicationSettings {

    private final TextField additionalParams = new TextField();

    public PushToEmacsSettings(PushToApplication application,
                               DialogService dialogService,
                               PreferencesService preferencesService,
                               ObjectProperty<PushToApplicationPreferences> preferences) {
        super(application, dialogService, preferencesService, preferences);

        settingsPane.add(new Label(Localization.lang("Additional parameters") + ":"), 0, 1);
        settingsPane.add(additionalParams, 1, 1);
        additionalParams.setText(preferences.get().getEmacsArguments());
    }

    @Override
    public void storeSettings() {
        super.storeSettings();
        preferences.setValue(preferences.getValue().withEmacsArguments(additionalParams.getText()));
    }
}
