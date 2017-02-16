package org.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.bibsonomy.BibSonomySettingsDialog;
import org.jabref.gui.bibsonomy.BibSonomySettingsDialog;
import org.jabref.logic.l10n.Localization;


/**
 * Creates and displays the {@link BibSonomySettingsDialog}
 */
public class ShowSettingsDialogAction extends AbstractAction {

    private JabRefFrame jabRefFrame;

    public void actionPerformed(ActionEvent e) {
        BibSonomySettingsDialog psd = new BibSonomySettingsDialog(jabRefFrame);
        psd.setVisible(true);
        psd.setLocationRelativeTo(jabRefFrame);
    }

    public ShowSettingsDialogAction(JabRefFrame jabRefFrame) {
        super(Localization.lang("Settings"), IconTheme.JabRefIcon.PREFERENCES.getIcon());
        this.jabRefFrame = jabRefFrame;
    }
}
