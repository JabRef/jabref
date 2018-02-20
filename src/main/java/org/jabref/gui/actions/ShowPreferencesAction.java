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
        // output(Localization.lang("Opening preferences..."));
        if (prefsDialog == null) {
            prefsDialog = new PreferencesDialog(jabRefFrame);
            //prefsDialog.setLocationRelativeTo(JabRefFrame.this);
        } else {
            prefsDialog.setValues();
        }

        prefsDialog.setVisible(true);
        //output("");

    }

}
