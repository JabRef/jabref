package org.jabref.gui.actions;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.dbproperties.DatabasePropertiesDialog;

public class DatabasePropertiesAction extends SimpleCommand {

    private final JabRefFrame frame;

    public DatabasePropertiesAction(JabRefFrame frame) {
        this.frame = frame;
    }

    @Override
    public void execute() {
        DatabasePropertiesDialog propertiesDialog = new DatabasePropertiesDialog(frame.getCurrentBasePanel());
        propertiesDialog.updateEnableStatus();
        propertiesDialog.setVisible(true);
    }

}
