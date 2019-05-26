package org.jabref.gui.push;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

public class PushToLyxSettings extends PushToApplicationSettings {

    public PushToLyxSettings (DialogService dialogService) { super(dialogService); }

    @Override
    protected void initJFXSettingsPanel() {
        super.initJFXSettingsPanel();
        path.setText(Globals.prefs.get(JabRefPreferences.LYXPIPE));
        commandLabel.setText(Localization.lang("Path to LyX pipe") + ":");
    }
}
