package org.jabref.gui.actions;

import javax.swing.JDialog;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.customentrytypes.EntryTypeCustomizationDialog;

public class CustomizeEntryAction extends SimpleCommand {

    private final JabRefFrame frame;

    public CustomizeEntryAction(JabRefFrame frame) {
        this.frame = frame;
    }

    @Override
    public void execute() {
        JDialog dialog = new EntryTypeCustomizationDialog(frame);
        dialog.setVisible(true);
    }
}
