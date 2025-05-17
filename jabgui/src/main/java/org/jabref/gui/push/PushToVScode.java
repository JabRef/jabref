package org.jabref.gui.push;

import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.GuiPreferences;

public class PushToVScode extends AbstractPushToApplication {

    public static final String NAME = PushToApplications.VSCODE;

    public PushToVScode(DialogService dialogService, GuiPreferences preferences) {
        super(dialogService, preferences);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public JabRefIcon getApplicationIcon() {
        return IconTheme.JabRefIcons.APPLICATION_VSCODE;
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        // TODO - Implementing this will fix https://github.com/JabRef/jabref/issues/6775
        return new String[] {commandPath};
    }

    @Override
    public String[] jumpToLineCommandlineArguments(Path fileName, int line, int column) {
        return new String[] {commandPath, "--g", "%s:%s:%s".formatted(fileName.toString(), line, column)};
    }
}
