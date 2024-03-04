package org.jabref.gui.push;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.preferences.PreferencesService;

public class PushToTeXworks extends AbstractPushToApplication {

    public static final String NAME = PushToApplications.TEXWORKS;

    /**
     * Constructs a new {@code PushToTeXworks} instance.
     *
     * @param dialogService The dialog service for displaying messages to the user.
     * @param preferencesService The service for accessing user preferences.
     */
    public PushToTeXworks(DialogService dialogService, PreferencesService preferencesService) {
        super(dialogService, preferencesService);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public JabRefIcon getApplicationIcon() {
        // TODO: replace the placeholder icon with the real one.
        return IconTheme.JabRefIcons.APPLICATION_GENERIC;
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath, "--insert-text", "%s%s%s".formatted(getCitePrefix(), keyString, getCiteSuffix())};
    }
}
