package org.jabref.gui.actions;

import org.jabref.gui.FindUnlinkedFilesDialog;
import org.jabref.gui.JabRefFrame;

public class FindUnlinkedFilesAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;

    public FindUnlinkedFilesAction(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Override
    public void execute() {
        FindUnlinkedFilesDialog dlg = new FindUnlinkedFilesDialog(jabRefFrame);
        dlg.setVisible(true);
    }

}
