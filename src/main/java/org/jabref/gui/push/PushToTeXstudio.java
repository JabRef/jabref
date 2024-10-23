package org.jabref.gui.push;

import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.GuiPreferences;

public class PushToTeXstudio extends AbstractPushToApplication {

    public static final String NAME = PushToApplications.TEXSTUDIO;

    public PushToTeXstudio(DialogService dialogService, GuiPreferences preferences) {
        super(dialogService, preferences);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public JabRefIcon getApplicationIcon() {
        return IconTheme.JabRefIcons.APPLICATION_TEXSTUDIO;
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath, "--insert-cite", "%s%s%s".formatted(getCitePrefix(), keyString, getCiteSuffix())};
    }

    @Override
    public String[] jumpToLineCommandlineArguments(Path fileName, int line, int column) {
        return new String[] {commandPath, "--line", Integer.toString(line), fileName.toString()};
    }
}
