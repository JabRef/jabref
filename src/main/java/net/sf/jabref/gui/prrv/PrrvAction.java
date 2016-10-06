package net.sf.jabref.gui.prrv;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.Icon;

import javafx.application.Platform;

import net.sf.jabref.gui.actions.MnemonicAwareAction;

public class PrrvAction extends MnemonicAwareAction {

    public PrrvAction(String title, String tooltip, Icon iconFile) {
        super(iconFile);
        putValue(Action.NAME, title);
        putValue(Action.SHORT_DESCRIPTION, tooltip);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Platform.runLater(() -> new PrrvDialogView().show());
    }
}