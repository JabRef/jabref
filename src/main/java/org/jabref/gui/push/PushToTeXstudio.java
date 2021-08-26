package org.jabref.gui.push;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.model.push.PushToApplicationConstants;
import org.jabref.preferences.PreferencesService;

public class PushToTeXstudio extends AbstractPushToApplication implements PushToApplication {

    public static final String NAME = PushToApplicationConstants.TEXSTUDIO;

    public PushToTeXstudio(DialogService dialogService, PreferencesService preferencesService) {
        super(dialogService, preferencesService);
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public JabRefIcon getIcon() {
        return IconTheme.JabRefIcons.APPLICATION_TEXSTUDIO;
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath, "--insert-cite", String.format("%s{%s}", getCiteCommand(), keyString)};
    }
}
