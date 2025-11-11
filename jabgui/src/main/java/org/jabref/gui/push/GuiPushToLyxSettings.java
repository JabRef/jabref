package org.jabref.gui.push;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.push.PushToApplication;
import org.jabref.logic.push.PushToApplicationPreferences;

public class GuiPushToLyxSettings extends GuiPushToApplicationSettings {

    public GuiPushToLyxSettings(PushToApplication application,
                                DialogService dialogService,
                                FilePreferences filePreferences,
                                PushToApplicationPreferences preferences) {
        super(application, dialogService, filePreferences, preferences);

        commandLabel.setText(Localization.lang("Path to LyX pipe") + ":");
    }
}
