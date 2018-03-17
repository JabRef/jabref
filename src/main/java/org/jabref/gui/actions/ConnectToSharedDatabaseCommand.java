package org.jabref.gui.actions;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.shared.ConnectToSharedDatabaseDialog;

/**
 * Opens a shared database.
 */
public class ConnectToSharedDatabaseCommand extends SimpleCommand {

    private final JabRefFrame jabRefFrame;

    public ConnectToSharedDatabaseCommand(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Override
    public void execute() {
        ConnectToSharedDatabaseDialog connectToSharedDatabaseDialog = new ConnectToSharedDatabaseDialog(jabRefFrame);
        connectToSharedDatabaseDialog.setVisible(true);
    }
}
