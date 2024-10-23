package org.jabref.gui.push;

import java.nio.file.Path;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.GuiPreferences;

public class PushToTeXworks extends AbstractPushToApplication {

    public static final String NAME = PushToApplications.TEXWORKS;

    /**
     * Constructs a new {@code PushToTeXworks} instance.
     *
     * @param dialogService The dialog service for displaying messages to the user.
     * @param preferences The service for accessing user preferences.
     */
    public PushToTeXworks(DialogService dialogService, GuiPreferences preferences) {
        super(dialogService, preferences);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public JabRefIcon getApplicationIcon() {
        return IconTheme.JabRefIcons.APPLICATION_TEXWORS;
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath, "--insert-text", "%s%s%s".formatted(getCitePrefix(), keyString, getCiteSuffix())};
    }

    @Override
    protected String[] jumpToLineCommandlineArguments(Path fileName, int line, int column) {
        // No command known to jump to a specific line
        return new String[] {commandPath, fileName.toString()};
    }
}
