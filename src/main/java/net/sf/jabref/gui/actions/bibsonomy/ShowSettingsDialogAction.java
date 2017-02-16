package net.sf.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.bibsonomy.BibSonomySettingsDialog;
import net.sf.jabref.logic.l10n.Localization;


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
