package org.bibsonomy.plugin.jabref.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import net.sf.jabref.logic.l10n.Localization;

import org.bibsonomy.plugin.jabref.gui.BibSonomySettingsDialog;

/**
 * {@link CloseBibSonomySettingsDialogByCancelAction} closes the {@link BibSonomySettingsDialog} without saving the properties
 *
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 */
public class CloseBibSonomySettingsDialogByCancelAction extends AbstractAction {

    private BibSonomySettingsDialog settingsDialog;

    public void actionPerformed(ActionEvent e) {
        settingsDialog.setVisible(false);
    }

    public CloseBibSonomySettingsDialogByCancelAction(BibSonomySettingsDialog settingsDialog) {
        super(Localization.lang("Cancel"), new ImageIcon(CloseBibSonomySettingsDialogByCancelAction.class.getResource("/images/images/cross.png")));
        this.settingsDialog = settingsDialog;
    }

}
