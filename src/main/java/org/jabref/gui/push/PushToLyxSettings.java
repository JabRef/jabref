package org.jabref.gui.push;

import javafx.scene.layout.GridPane;

import org.jabref.Globals;
import org.jabref.preferences.JabRefPreferences;

public class PushToLyxSettings extends PushToApplicationSettings {

    @Override
    public GridPane getJFXSettingPane(int n) {
        path1.setText(Globals.prefs.get(JabRefPreferences.LYXPIPE));
        return super.getJFXSettingPane(n);
    }
}
