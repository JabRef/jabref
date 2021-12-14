package org.jabref.gui.sidepane;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.preferences.SidePanePreferences;

public class TogglePaneAction extends SimpleCommand {
    private final StateManager stateManager;
    private final SidePaneType pane;
    private final SidePanePreferences sidePanePreferences;

    public TogglePaneAction(StateManager stateManager, SidePaneType pane, SidePanePreferences sidePanePreferences) {
        this.stateManager = stateManager;
        this.pane = pane;
        this.sidePanePreferences = sidePanePreferences;
    }

    @Override
    public void execute() {
        if (!stateManager.getVisibleSidePaneComponents().contains(pane)) {
            stateManager.getVisibleSidePaneComponents().add(pane);
            stateManager.getVisibleSidePaneComponents().sort(new SidePaneViewModel.PreferredIndexSort(sidePanePreferences));
        } else {
            stateManager.getVisibleSidePaneComponents().remove(pane);
        }
    }
}
