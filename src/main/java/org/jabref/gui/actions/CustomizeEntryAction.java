package org.jabref.gui.actions;

import javax.swing.JDialog;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.customentrytypes.EntryCustomizationDialog;

public class CustomizeEntryAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;

    public CustomizeEntryAction(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }
    @Override
    public void execute() {
        JDialog dl = new EntryCustomizationDialog(jabRefFrame);
        dl.setVisible(true);

    }

}
