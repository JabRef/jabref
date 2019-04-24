package org.jabref.gui.libraryproperties;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class LibraryPropertiesAction extends SimpleCommand {

    private final JabRefFrame frame;
    private final DialogService dialogService;

    public LibraryPropertiesAction(JabRefFrame frame, DialogService dialogService, StateManager stateManager) {
        this.frame = frame;
        this.dialogService = dialogService;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        LibraryPropertiesDialogView propertiesDialog = new LibraryPropertiesDialogView(frame.getCurrentBasePanel(), dialogService);
        propertiesDialog.showAndWait();

    }

}
