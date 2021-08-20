package org.jabref.gui.push;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.model.push.PushToApplicationConstants;
import org.jabref.preferences.PreferencesService;

public class PushToWinEdt extends AbstractPushToApplication implements PushToApplication {

    public static final String NAME = PushToApplicationConstants.WIN_EDT;

    public PushToWinEdt(DialogService dialogService, PreferencesService preferencesService) {
        super(dialogService, preferencesService);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public JabRefIcon getIcon() {
        return IconTheme.JabRefIcons.APPLICATION_WINEDT;
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath,
                "\"[InsText('" + getCiteCommand() + "{" + keyString.replace("'", "''") + "}');]\""};
    }
}
