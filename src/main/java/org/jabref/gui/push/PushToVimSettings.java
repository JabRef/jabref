package org.jabref.gui.push;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import org.jabref.Globals;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

public class PushToVimSettings extends PushToApplicationSettings {

    private final TextField vimServer = new TextField();

    @Override
    public void storeSettings() {
        super.storeSettings();
        Globals.prefs.put(JabRefPreferences.VIM_SERVER, vimServer.getText());
    }

    @Override
    protected void initJFXSettingsPanel() {
        super.initJFXSettingsPanel();
        jfxSettings.add(new Label(Localization.lang("Vim server name") + ":"), 0, 1);
        jfxSettings.add(vimServer, 1, 1);
        vimServer.setText(Globals.prefs.get(JabRefPreferences.VIM_SERVER));
    }
}
