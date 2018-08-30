package org.jabref.gui.push;

import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jabref.Globals;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

public class PushToVimSettings extends PushToApplicationSettings {

    private final JTextField vimServer = new JTextField(30);

    @Override
    public JPanel getSettingsPanel(int n) {
        vimServer.setText(Globals.prefs.get(JabRefPreferences.VIM_SERVER));
        return super.getSettingsPanel(n);
    }

    @Override
    public void storeSettings() {
        super.storeSettings();
        Globals.prefs.put(JabRefPreferences.VIM_SERVER, vimServer.getText());
    }

    @Override
    protected void initSettingsPanel() {
        super.initSettingsPanel();
        builder.appendRows("2dlu, p");
        builder.add(Localization.lang("Vim server name") + ":").xy(1, 3);
        builder.add(vimServer).xy(3, 3);
        settings = builder.build();
    }
}
