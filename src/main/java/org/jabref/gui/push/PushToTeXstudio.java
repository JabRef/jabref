package org.jabref.gui.push;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.preferences.JabRefPreferences;

public class PushToTeXstudio extends AbstractPushToApplication implements PushToApplication {

    public PushToTeXstudio(DialogService dialogService) {
        super(dialogService);
    }

    @Override
    public String getApplicationName() {
        return "TeXstudio";
    }

    @Override
    public JabRefIcon getIcon() {
        return IconTheme.JabRefIcons.APPLICATION_TEXSTUDIO;
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath, "--insert-cite", String.format("%s{%s}", getCiteCommand(), keyString)};
    }

    @Override
    protected void initParameters() {
        commandPathPreferenceKey = JabRefPreferences.TEXSTUDIO_PATH;
    }
}
