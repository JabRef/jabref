package org.jabref.gui.help;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.Icon;

import javafx.application.Platform;

import org.jabref.gui.actions.MnemonicAwareAction;

public class AboutAction extends MnemonicAwareAction {

    public AboutAction(String title, String tooltip, Icon iconFile) {
        super(iconFile);
        putValue(Action.NAME, title);
        putValue(Action.SHORT_DESCRIPTION, tooltip);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Platform.runLater(() -> new AboutDialogView().show());
    }
}
