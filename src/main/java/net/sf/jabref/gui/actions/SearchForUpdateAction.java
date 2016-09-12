package net.sf.jabref.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import net.sf.jabref.JabRefGUI;
import net.sf.jabref.logic.l10n.Localization;

public class SearchForUpdateAction extends AbstractAction {

    public SearchForUpdateAction(){
        super(Localization.lang("Check for updates"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JabRefGUI.checkForNewVersion(true);
    }
}
