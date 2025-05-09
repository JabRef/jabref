package org.jabref.gui.push;

import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.GuiPreferences;

/**
 * Class for pushing entries into TexMaker.
 */
public class PushToTexmaker extends AbstractPushToApplication {

    public static final String NAME = "Texmaker";

    public PushToTexmaker(DialogService dialogService, GuiPreferences preferences) {
        super(dialogService, preferences);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public JabRefIcon getApplicationIcon() {
        return IconTheme.JabRefIcons.APPLICATION_TEXMAKER;
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath, "-insert", getCitePrefix() + keyString + getCiteSuffix()};
    }

    @Override
    protected String[] jumpToLineCommandlineArguments(Path fileName, int line, int column) {
        return new String[] {commandPath, "-line", Integer.toString(line), fileName.toString()};
    }
}
