package net.sf.jabref.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.shared.OpenSharedDatabaseDialog;
import net.sf.jabref.logic.l10n.Localization;

/**
 * The action concerned with opening a shared database.
 */
public class OpenSharedDatabaseAction extends MnemonicAwareAction {

    private final JabRefFrame jabRefFrame;


    public OpenSharedDatabaseAction(JabRefFrame jabRefFrame) {
        super();
        this.jabRefFrame = jabRefFrame;
        putValue(Action.NAME, Localization.menuTitle("Open shared database"));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Open shared database"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        OpenSharedDatabaseDialog openSharedDatabaseDialog = new OpenSharedDatabaseDialog(jabRefFrame);
        openSharedDatabaseDialog.setVisible(true);
    }
}
