package org.jabref.gui.actions;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.dbproperties.DatabasePropertiesDialog;

public class LibraryPropertiesAction extends SimpleCommand {

    private final JabRefFrame frame;
    private DatabasePropertiesDialog propertiesDialog;

    public LibraryPropertiesAction(JabRefFrame frame) {
        this.frame = frame;
    }

    @Override
    public void execute() {
        if (propertiesDialog == null) {
            propertiesDialog = new DatabasePropertiesDialog(null);
        }

        propertiesDialog.setPanel(frame.getCurrentBasePanel());
        propertiesDialog.updateEnableStatus();
        propertiesDialog.setLocationRelativeTo(null);
        propertiesDialog.setVisible(true);
    }

}
