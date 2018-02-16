package org.jabref.gui.actions;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.dbproperties.DatabasePropertiesDialog;

public class LibraryPropertiesAction extends SimpleCommand {

    private DatabasePropertiesDialog propertiesDialog;
    private final BasePanel basePanel;

    public LibraryPropertiesAction(JabRefFrame jabrefFrame) {
        this.basePanel = jabrefFrame.getCurrentBasePanel();
    }

    @Override
    public void execute() {
        if (propertiesDialog == null) {
            propertiesDialog = new DatabasePropertiesDialog(null);
        }
        propertiesDialog.setPanel(basePanel);
        propertiesDialog.updateEnableStatus();
        propertiesDialog.setLocationRelativeTo(null);
        propertiesDialog.setVisible(true);
    }

}
