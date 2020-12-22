package org.jabref.gui.push;

import javafx.beans.property.ObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.PushToApplicationPreferences;

public class PushToVimSettings extends PushToApplicationSettings {

    private final TextField vimServer = new TextField();

    public PushToVimSettings(PushToApplication application,
                             DialogService dialogService,
                             PreferencesService preferencesService,
                             ObjectProperty<PushToApplicationPreferences> preferences) {
        super(application, dialogService, preferencesService, preferences);

        settingsPane.add(new Label(Localization.lang("Vim server name") + ":"), 0, 1);
        settingsPane.add(vimServer, 1, 1);
        vimServer.setText(preferences.get().getVimServer());
    }

    @Override
    public void storeSettings() {
        super.storeSettings();
        preferences.setValue(preferences.get().withVimServer(vimServer.getText()));
    }
}
