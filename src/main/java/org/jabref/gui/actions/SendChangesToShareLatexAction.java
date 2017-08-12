package org.jabref.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jabref.Globals;
import org.jabref.gui.StateManager;
import org.jabref.logic.sharelatex.ShareLatexManager;

public class SendChangesToShareLatexAction extends AbstractAction {

    public SendChangesToShareLatexAction() {
        super();
        putValue(Action.NAME, "Send changes to ShareLaTeX Server");

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        ShareLatexManager manager = Globals.shareLatexManager;
        StateManager stateManager = Globals.stateManager;
        manager.sendNewDatabaseContent(stateManager.getActiveDatabase().get());
        System.out.println("Send changes");
    }

}
