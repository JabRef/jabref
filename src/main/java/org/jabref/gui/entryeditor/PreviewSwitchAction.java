package org.jabref.gui.entryeditor;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class PreviewSwitchAction extends SimpleCommand {

    public enum Direction { PREVIOUS, NEXT }

    private final JabRefFrame frame;
    private final Direction direction;

    public PreviewSwitchAction(Direction direction, JabRefFrame frame, StateManager stateManager) {
        this.frame = frame;
        this.direction = direction;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        if (direction == Direction.NEXT) {
            frame.getCurrentLibraryTab().getEntryEditor().nextPreviewStyle();
        } else {
            frame.getCurrentLibraryTab().getEntryEditor().previousPreviewStyle();
        }
    }
}
