package org.jabref.gui.actions;

import org.jabref.JabRefGUI;

public class SearchForUpdateAction extends SimpleCommand {

    @Override
    public void execute() {
        JabRefGUI.checkForNewVersion(true);
    }
}
