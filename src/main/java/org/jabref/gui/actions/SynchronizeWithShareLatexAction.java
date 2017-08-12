package org.jabref.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import javafx.application.Platform;

import org.jabref.gui.IconTheme;
import org.jabref.gui.sharelatex.ShareLatexLoginDialogView;

public class SynchronizeWithShareLatexAction extends AbstractAction {

    public SynchronizeWithShareLatexAction() {
        super();
        putValue(Action.NAME, "Synchronize with ShareLaTeX");
        putValue(Action.SMALL_ICON, IconTheme.getImage("sharelatex"));
        putValue(Action.SHORT_DESCRIPTION, "Synchronize with ShareLaTeX");

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Platform.runLater(() -> new ShareLatexLoginDialogView().show());

    }
}
