package net.sf.jabref.gui.push;

import javax.swing.Icon;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.preferences.JabRefPreferences;

/**
 * Class for pushing entries into LatexEditor.
 */
public class PushToLatexEditor extends AbstractPushToApplication implements PushToApplication {

    @Override
    public String getApplicationName() {
        return "LatexEditor";
    }

    @Override
    public Icon getIcon() {
        return IconTheme.JabRefIcon.EDIT.getSmallIcon();
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath, "-i", getCiteCommand() + "{" + keyString + "}"};
    }

    @Override
    protected String getCommandName() {
        return "LEd.exe";
    }

    @Override
    protected void initParameters() {
        commandPathPreferenceKey = JabRefPreferences.LATEX_EDITOR_PATH;
    }

}
