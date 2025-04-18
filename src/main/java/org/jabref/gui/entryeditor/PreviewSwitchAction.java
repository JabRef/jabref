package org.jabref.gui.entryeditor;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preview.PreviewControls;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class PreviewSwitchAction extends SimpleCommand {

    public enum Direction { PREVIOUS, NEXT }

    private final PreviewControls previewControls;
    private final Direction direction;

    public PreviewSwitchAction(Direction direction, PreviewControls previewControls, StateManager stateManager) {
        this.previewControls = previewControls;
        this.direction = direction;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        if (direction == Direction.NEXT) {
            previewControls.nextPreviewStyle();
        } else {
            previewControls.previousPreviewStyle();
        }
    }
}
