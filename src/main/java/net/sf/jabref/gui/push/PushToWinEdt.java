package net.sf.jabref.gui.push;

import javax.swing.Icon;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.preferences.JabRefPreferences;

public class PushToWinEdt extends AbstractPushToApplication implements PushToApplication {

    @Override
    public String getApplicationName() {
        return "WinEdt";
    }

    @Override
    public Icon getIcon() {
        return IconTheme.getImage("winedt");
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
