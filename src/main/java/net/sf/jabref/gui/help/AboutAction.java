package net.sf.jabref.gui.help;

import java.awt.Frame;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.Icon;

import net.sf.jabref.gui.actions.MnemonicAwareAction;

public class AboutAction extends MnemonicAwareAction {
    private final Frame parentFrame;

    public AboutAction(String title, Frame parentFrame, String tooltip, Icon iconFile) {
        super(iconFile);
        putValue(Action.NAME, title);
        putValue(Action.SHORT_DESCRIPTION, tooltip);
        this.parentFrame = parentFrame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        AboutDialog dialog = new AboutDialog(parentFrame);
        dialog.setVisible(true);
    }
}
