package org.jabref.gui.push;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.push.PushToApplication;
import org.jabref.logic.push.PushToApplicationPreferences;

public class GuiPushToVimSettings extends GuiPushToApplicationSettings {

    private final TextField vimServer = new TextField();

    public GuiPushToVimSettings(PushToApplication application,
                                DialogService dialogService,
                                FilePreferences filePreferences,
                                PushToApplicationPreferences preferences) {
        super(application, dialogService, filePreferences, preferences);

        settingsPane.add(new Label(Localization.lang("Vim server name") + ":"), 0, 1);
        settingsPane.add(vimServer, 1, 1);
        vimServer.setText(preferences.getVimServer());
    }

    @Override
    public void storeSettings() {
        super.storeSettings();
        preferences.setVimServer(vimServer.getText());
    }
}
