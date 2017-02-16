package net.sf.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.bibsonomy.BibSonomySettingsDialog;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Closes the {@link BibSonomySettingsDialog} without saving the properties
 */
public class CloseBibSonomySettingsDialogByCancelAction extends AbstractAction {

    private BibSonomySettingsDialog settingsDialog;

    public void actionPerformed(ActionEvent e) {
        settingsDialog.setVisible(false);
    }

    public CloseBibSonomySettingsDialogByCancelAction(BibSonomySettingsDialog settingsDialog) {
        super(Localization.lang("Cancel"), IconTheme.JabRefIcon.CANCEL.getIcon());
        this.settingsDialog = settingsDialog;
    }

}
