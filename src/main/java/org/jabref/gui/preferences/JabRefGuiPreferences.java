package org.jabref.gui.preferences;

import org.jabref.logic.preferences.JabRefPreferences;

public class JabRefGuiPreferences extends JabRefPreferences implements GuiPreferences {

    private static JabRefGuiPreferences singleton;

    @Deprecated
    public static JabRefGuiPreferences getInstance() {
        if (JabRefGuiPreferences.singleton == null) {
            JabRefGuiPreferences.singleton = new JabRefGuiPreferences();
        }
        return JabRefGuiPreferences.singleton;
    }
}
