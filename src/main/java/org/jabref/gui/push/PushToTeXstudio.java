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

    /**
     * Method to open TeXstudio at the given line number in the specified LaTeX file.
     */
    @Override
    public String[] jumpString(Path fileName, int line, int column) {
        // Construct the TeXstudio command
        return new String[] {commandPath, "--line", Integer.toString(line), fileName.toString()};
    }
}
