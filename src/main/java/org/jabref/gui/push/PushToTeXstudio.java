package org.jabref.gui.push;

import javax.swing.Icon;

import org.jabref.gui.IconTheme;
import org.jabref.preferences.JabRefPreferences;

public class PushToTeXstudio extends AbstractPushToApplication implements PushToApplication {

    @Override
    public String getApplicationName() {
        return "TeXstudio";
    }

    @Override
    public Icon getIcon() {
        return IconTheme.getImage("texstudio");
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath, "--insert-cite", String.format("%s{%s}", getCiteCommand(), keyString)};
    }

    @Override
    protected void initParameters() {
        commandPathPreferenceKey = JabRefPreferences.TEXSTUDIO_PATH;
    }
}
