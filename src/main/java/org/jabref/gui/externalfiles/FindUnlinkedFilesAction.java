package org.jabref.gui.externalfiles;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;

public class FindUnlinkedFilesAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;

    public FindUnlinkedFilesAction(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Override
    public void execute() {
        FindUnlinkedFilesDialog dlg = new FindUnlinkedFilesDialog(jabRefFrame);
        dlg.showAndWait();
    }

}
