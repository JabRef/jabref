package org.jabref.gui.entryeditor;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;

public class OpenEntryEditorAction extends SimpleCommand {

    private final JabRefFrame frame;
    private final StateManager stateManager;

    public OpenEntryEditorAction(JabRefFrame frame, StateManager stateManager) {
        this.frame = frame;
        this.stateManager = stateManager;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    public void execute() {
        if (!stateManager.getSelectedEntries().isEmpty()) {
            frame.getCurrentLibraryTab().showAndEdit(stateManager.getSelectedEntries().get(0));
        }
    }
}
