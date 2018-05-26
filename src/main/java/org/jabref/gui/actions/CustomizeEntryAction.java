package org.jabref.gui.actions;

import javax.swing.JDialog;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.customentrytypes.EntryCustomizationDialog;

public class CustomizeEntryAction extends SimpleCommand {

    private final JabRefFrame frame;

    public CustomizeEntryAction(JabRefFrame frame) {
        this.frame = frame;
    }
    
    @Override
    public void execute() {
        JDialog dialog = new EntryCustomizationDialog(frame);
        dialog.setVisible(true);
    }
}
