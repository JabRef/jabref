package org.jabref.gui.push;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

public class PushToVimSettings extends PushToApplicationSettings {

    private final TextField vimServer = new TextField();

    public PushToVimSettings(PushToApplication application, DialogService dialogService) {
        super(application, dialogService);
        settingsPane.add(new Label(Localization.lang("Vim server name") + ":"), 0, 1);
        settingsPane.add(vimServer, 1, 1);
        vimServer.setText(Globals.prefs.get(JabRefPreferences.VIM_SERVER));
    }

    @Override
    public void storeSettings() {
        super.storeSettings();
        Globals.prefs.put(JabRefPreferences.VIM_SERVER, vimServer.getText());
    }
}
