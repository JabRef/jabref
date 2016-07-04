package net.sf.jabref.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.OpenRemoteDatabaseDialog;
import net.sf.jabref.logic.l10n.Localization;

/**
 * The action concerned with opening a remote database.
 */
public class OpenRemoteDatabaseAction extends MnemonicAwareAction {

    private final JabRefFrame jabRefFrame;


    public OpenRemoteDatabaseAction(JabRefFrame jabRefFrame) {
        super();
        this.jabRefFrame = jabRefFrame;
        putValue(Action.NAME, Localization.menuTitle("Open remote database"));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Open remote database"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        OpenRemoteDatabaseDialog openRemoteDBDialog = new OpenRemoteDatabaseDialog(jabRefFrame);
        openRemoteDBDialog.setLocationRelativeTo(jabRefFrame);
        openRemoteDBDialog.setVisible(true);
    }
}
