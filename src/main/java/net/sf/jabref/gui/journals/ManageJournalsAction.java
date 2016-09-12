package net.sf.jabref.gui.journals;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.logic.l10n.Localization;

public class ManageJournalsAction extends MnemonicAwareAction {

    private final JabRefFrame frame;

    public ManageJournalsAction(JabRefFrame frame) {
        super();
        putValue(Action.NAME, Localization.menuTitle("Manage journal abbreviations"));
        this.frame = frame;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        ManageJournalsPanel panel = new ManageJournalsPanel(frame);
        panel.getDialog().setLocationRelativeTo(frame);
        panel.setValues();
        panel.getDialog().setVisible(true);
    }
}
