package org.jabref.gui.push;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;

public class PushToLyxSettings extends PushToApplicationSettings {

    public PushToLyxSettings(PushToApplication application,
                             DialogService dialogService,
                             FilePreferences filePreferences,
                             PushToApplicationPreferences preferences) {
        super(application, dialogService, filePreferences, preferences);

        commandLabel.setText(Localization.lang("Path to LyX pipe") + ":");
    }
}
