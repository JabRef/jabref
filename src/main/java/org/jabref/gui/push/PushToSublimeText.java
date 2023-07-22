package org.jabref.gui.push;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.preferences.PreferencesService;

/**
 * Class for pushing entries into SublimeText.
 */
public class PushToSublimeText extends AbstractPushToApplication {

    public static final String NAME = "SublimeText";

    public PushToSublimeText(DialogService dialogService, PreferencesService preferencesService) {
        super(dialogService, preferencesService);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public JabRefIcon getApplicationIcon() {
        return IconTheme.JabRefIcons.APPLICATION_SUBLIMETEXT;
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath, "--command \'insert {\"characters\": \"\\", getCiteCommand() + "{" + keyString + "}\\}\'"};
    }
}
