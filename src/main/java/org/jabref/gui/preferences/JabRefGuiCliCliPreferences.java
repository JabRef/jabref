package org.jabref.gui.preferences;

import org.jabref.logic.preferences.JabRefCliCliPreferences;

public class JabRefGuiCliCliPreferences extends JabRefCliCliPreferences implements GuiCliPreferences {

    private static JabRefGuiCliCliPreferences singleton;

    @Deprecated
    public static JabRefGuiCliCliPreferences getInstance() {
        if (JabRefGuiCliCliPreferences.singleton == null) {
            JabRefGuiCliCliPreferences.singleton = new JabRefGuiCliCliPreferences();
        }
        return JabRefGuiCliCliPreferences.singleton;
    }
}
