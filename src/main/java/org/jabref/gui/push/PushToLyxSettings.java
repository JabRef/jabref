package org.jabref.gui.push;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jabref.logic.l10n.Localization;

public class PushToLyxSettings extends PushToApplicationSettings {

    @Override
    protected void initSettingsPanel() {
        super.initSettingsPanel();
        settings = new JPanel();
        settings.add(new JLabel(Localization.lang("Path to LyX pipe") + ":"));
        settings.add(path);
    }
}
