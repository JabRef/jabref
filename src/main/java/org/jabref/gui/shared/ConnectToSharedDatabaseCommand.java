package org.jabref.gui.shared;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;

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
        new SharedDatabaseLoginDialogView(jabRefFrame).showAndWait();
    }
}
