package org.jabref.gui.push;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import org.jabref.Globals;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

public class PushToEmacsSettings extends PushToApplicationSettings {

    private final TextField additionalParams = new TextField();

    @Override
    public void storeSettings() {
        super.storeSettings();
        Globals.prefs.put(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS, additionalParams.getText());
    }

    @Override
    protected void initJFXSettingsPanel() {
        super.initJFXSettingsPanel();
        jfxSettings.add(new Label(Localization.lang("Additional parameters") + ":"), 0, 1);
        jfxSettings.add(additionalParams, 1, 1);
        additionalParams.setText(Globals.prefs.get(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS));
    }
}
