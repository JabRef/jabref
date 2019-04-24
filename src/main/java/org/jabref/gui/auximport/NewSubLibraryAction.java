package org.jabref.gui.auximport;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

/**
 * The action concerned with generate a new (sub-)database from latex AUX file.
 */
public class NewSubLibraryAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;

    public NewSubLibraryAction(JabRefFrame jabRefFrame, StateManager stateManager) {
        this.jabRefFrame = jabRefFrame;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        FromAuxDialog dialog = new FromAuxDialog(jabRefFrame);
        dialog.showAndWait();
    }
}
