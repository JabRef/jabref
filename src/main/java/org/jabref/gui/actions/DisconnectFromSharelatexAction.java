package org.jabref.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jabref.Globals;

public class DisconnectFromSharelatexAction extends AbstractAction {

    public DisconnectFromSharelatexAction() {
        super();
        putValue(Action.NAME, "Disconnect from ShareLaTeX");

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Globals.shareLatexManager.disconnectAndCloseConnection();
    }

}
