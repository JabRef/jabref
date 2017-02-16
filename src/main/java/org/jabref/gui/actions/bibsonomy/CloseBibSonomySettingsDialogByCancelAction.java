package org.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jabref.gui.IconTheme;
import org.jabref.gui.bibsonomy.BibSonomySettingsDialog;
import org.jabref.logic.l10n.Localization;

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
