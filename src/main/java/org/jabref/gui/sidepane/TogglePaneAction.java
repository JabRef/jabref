package org.jabref.gui.sidepane;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

public class TogglePaneAction extends SimpleCommand {
    private final StateManager stateManager;
    private final SidePaneType pane;

    public TogglePaneAction(StateManager stateManager, SidePaneType pane) {
        this.stateManager = stateManager;
        this.pane = pane;
    }

    @Override
    public void execute() {
        boolean isVisible = stateManager.sidePaneComponentVisiblePropertyFor(pane).get();
        stateManager.sidePaneComponentVisiblePropertyFor(pane).set(!isVisible);
    }
}
