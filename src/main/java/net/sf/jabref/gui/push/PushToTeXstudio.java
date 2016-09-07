package net.sf.jabref.gui.push;

import javax.swing.Icon;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.preferences.JabRefPreferences;

/**
 * Created by IntelliJ IDEA. User: alver Date: Jan 14, 2006 Time: 4:55:23 PM To change this template use File | Settings
 * | File Templates.
 */
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
