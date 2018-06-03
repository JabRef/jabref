package org.jabref.gui.actions;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.preftabs.PreferencesDialog;

public class ShowPreferencesAction extends SimpleCommand {

    private PreferencesDialog prefsDialog;
    private final JabRefFrame jabRefFrame;

    public ShowPreferencesAction(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Override
    public void execute() {
        if (prefsDialog == null) {
            prefsDialog = new PreferencesDialog(jabRefFrame);
        } else {
            prefsDialog.setValues();
        }

        prefsDialog.showAndWait();
    }
}
