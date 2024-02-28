package org.jabref.gui.push;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.preferences.PreferencesService;

public class PushToTeXworks extends AbstractPushToApplication {

    public static final String NAME = PushToApplications.TEXWORKS;

    public PushToTeXworks(DialogService dialogService, PreferencesService preferencesService) {
        super(dialogService, preferencesService);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public JabRefIcon getApplicationIcon() {
        return IconTheme.JabRefIcons.DEFAULT_GROUP_ICON; // Temporary Icon that needs to be changed
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath, "--insert-text", "%s%s%s".formatted(getCitePrefix(), keyString, getCiteSuffix())};
    }
}
