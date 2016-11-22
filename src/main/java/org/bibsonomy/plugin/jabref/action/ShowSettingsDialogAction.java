package org.bibsonomy.plugin.jabref.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;

import org.bibsonomy.plugin.jabref.gui.BibSonomySettingsDialog;


/**
 * {@link ShowSettingsDialogAction} creates and displays the {@link BibSonomySettingsDialog}
 *
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 */
public class ShowSettingsDialogAction extends AbstractAction {

    private JabRefFrame jabRefFrame;

    public void actionPerformed(ActionEvent e) {
        BibSonomySettingsDialog psd = new BibSonomySettingsDialog(jabRefFrame);
        psd.setVisible(true);
        psd.setLocationRelativeTo(jabRefFrame);
    }

    public ShowSettingsDialogAction(JabRefFrame jabRefFrame) {
        super(Localization.lang("Settings"), new ImageIcon(ShowSettingsDialogAction.class.getResource("/images/images/wrench-screwdriver.png")));
        this.jabRefFrame = jabRefFrame;
    }
}
