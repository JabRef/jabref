package org.jabref.gui.edit;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;

public class ReplaceStringAction extends SimpleCommand {
    private final JabRefFrame frame;

    public ReplaceStringAction(JabRefFrame frame, StateManager stateManager) {
        this.frame = frame;

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        ReplaceStringView dialog = new ReplaceStringView(frame.getCurrentLibraryTab());
        dialog.showAndWait();
    }
}
