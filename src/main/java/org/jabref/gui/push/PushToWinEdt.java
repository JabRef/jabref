package org.jabref.gui.push;

import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefIcon;
import org.jabref.preferences.JabRefPreferences;

public class PushToWinEdt extends AbstractPushToApplication implements PushToApplication {

    @Override
    public String getApplicationName() {
        return "WinEdt";
    }

    @Override
    public JabRefIcon getIcon() {
        return IconTheme.JabRefIcons.APPLICATION_WINEDT;
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath,
                "\"[InsText('" + getCiteCommand() + "{" + keyString.replace("'", "''") + "}');]\""};
    }

    @Override
    protected void initParameters() {
        commandPathPreferenceKey = JabRefPreferences.WIN_EDT_PATH;
    }
}
