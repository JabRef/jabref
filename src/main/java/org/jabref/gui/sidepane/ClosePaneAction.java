package org.jabref.gui.sidepane;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

public class ClosePaneAction extends SimpleCommand {
    private final StateManager stateManager;
    private final SidePaneType toClosePane;

    public ClosePaneAction(StateManager stateManager, SidePaneType toClosePane) {
        this.stateManager = stateManager;
        this.toClosePane = toClosePane;
    }

    @Override
    public void execute() {
        stateManager.sidePaneComponentVisiblePropertyFor(toClosePane).set(false);
    }
}
