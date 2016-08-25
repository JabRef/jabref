package net.sf.jabref.gui.push;

import javax.swing.Icon;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.preferences.JabRefPreferences;

/**
 * Class for pushing entries into TexMaker.
 */
public class PushToTexmaker extends AbstractPushToApplication implements PushToApplication {

    @Override
    public String getApplicationName() {
        return "Texmaker";
    }

    @Override
    public Icon getIcon() {
        return IconTheme.getImage("texmaker");
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath, "-insert", getCiteCommand() + "{" + keyString + "}"};
    }

    @Override
    protected void initParameters() {
        commandPathPreferenceKey = JabRefPreferences.TEXMAKER_PATH;
    }

}
