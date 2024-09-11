package org.jabref.gui.push;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.GuiPreferences;

public class PushToWinEdt extends AbstractPushToApplication {

    public static final String NAME = PushToApplications.WIN_EDT;

    public PushToWinEdt(DialogService dialogService, GuiPreferences preferences) {
        super(dialogService, preferences);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public JabRefIcon getApplicationIcon() {
        return IconTheme.JabRefIcons.APPLICATION_WINEDT;
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath,
                "\"[InsText('" + getCitePrefix() + keyString.replace("'", "''") + getCiteSuffix() + "');]\""};
    }
}
