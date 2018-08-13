package org.jabref.gui.push;

import javax.swing.JPanel;
import javax.swing.JTextField;

import javafx.scene.layout.GridPane;

import org.jabref.Globals;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

public class PushToEmacsSettings extends PushToApplicationSettings {

    private final JTextField additionalParams = new JTextField(30);

    @Override
    public JPanel getSettingsPanel(int n) {
        additionalParams.setText(Globals.prefs.get(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS));
        if (super.settings == null) {
            super.initSettingsPanel("Emacs",Globals.prefs.get(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS));
        }
        super.path.setText(Globals.prefs.get(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS));
        return super.settings;
    }

    @Override
    public GridPane getJFXSettingPane() {
        additionalParams.setText(Globals.prefs.get(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS));
        return super.getJFXSettingPane();
    }

    @Override
    public void storeSettings() {
        super.storeSettings();
        Globals.prefs.put(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS, additionalParams.getText());
    }

    @Override
    protected void initSettingsPanel(String s, String command) {
        super.initSettingsPanel(s,Globals.prefs.get(JabRefPreferences.EMACS_ADDITIONAL_PARAMETERS));
        builder.appendRows("2dlu, p, 2dlu, p");
        builder.add(Localization.lang("Additional parameters") + ":").xy(1, 3);
        builder.add(additionalParams).xy(3, 3);
        settings = builder.build();
    }
}
