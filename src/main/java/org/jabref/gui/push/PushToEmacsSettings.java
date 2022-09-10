package org.jabref.gui.push;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PushToApplicationPreferences;

public class PushToEmacsSettings extends PushToApplicationSettings {

    private final TextField additionalParams = new TextField();

    public PushToEmacsSettings(PushToApplication application,
                               DialogService dialogService,
                               FilePreferences filePreferences,
                               PushToApplicationPreferences preferences) {
        super(application, dialogService, filePreferences, preferences);

        settingsPane.add(new Label(Localization.lang("Additional parameters") + ":"), 0, 1);
        settingsPane.add(additionalParams, 1, 1);
        additionalParams.setText(preferences.getEmacsArguments());
    }

    @Override
    public void storeSettings() {
        super.storeSettings();
        preferences.setEmacsArguments(additionalParams.getText());
    }
}
