package org.jabref.gui.libraryproperties;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class LibraryPropertiesAction extends SimpleCommand {

    private final JabRefFrame frame;

    public LibraryPropertiesAction(JabRefFrame frame, StateManager stateManager) {
        this.frame = frame;
        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        LibraryPropertiesDialogView propertiesDialog = new LibraryPropertiesDialogView(frame.getCurrentLibraryTab());
        propertiesDialog.showAndWait();
    }
}
