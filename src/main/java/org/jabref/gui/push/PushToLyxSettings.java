package org.jabref.gui.push;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

public class PushToLyxSettings extends PushToApplicationSettings {

    public PushToLyxSettings(PushToApplication application, DialogService dialogService) {
        super(application, dialogService);
        path.setText(Globals.prefs.get(JabRefPreferences.LYXPIPE));
        commandLabel.setText(Localization.lang("Path to LyX pipe") + ":");
    }
}
