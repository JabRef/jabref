package org.jabref.gui.push;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

public class PushToEmacsSettings extends PushToApplicationSettings {

    private final TextField additionalParams = new TextField();

    public PushToEmacsSettings(PushToApplication application, DialogService dialogService) {
        super(application, dialogService);
        settingsPane.add(new Label(Localization.lang("Additional parameters") + ":"), 0, 1);
        settingsPane.add(additionalParams, 1, 1);
        additionalParams.setText(Globals.prefs.get(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS));
    }

    @Override
    public void storeSettings() {
        super.storeSettings();
        Globals.prefs.put(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS, additionalParams.getText());
    }
}
