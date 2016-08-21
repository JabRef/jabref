package net.sf.jabref.gui.help;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.Icon;

import net.sf.jabref.gui.actions.MnemonicAwareAction;

public class AboutAction extends MnemonicAwareAction {
    private final AboutDialog dialog;

    public AboutAction(String title, AboutDialog dialog, String tooltip, Icon iconFile) {
        super(iconFile);
        putValue(Action.NAME, title);
        putValue(Action.SHORT_DESCRIPTION, tooltip);
        this.dialog = dialog;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dialog.setVisible(true);
    }
}
