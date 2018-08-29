package org.jabref.gui.actions;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.preferences.PreferencesDialog;

public class ShowPreferencesAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;
    public ShowPreferencesAction(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Override
    public void execute() {
        PreferencesDialog preferencesDialog = new PreferencesDialog(jabRefFrame);
        preferencesDialog.showAndWait();
    }
}
