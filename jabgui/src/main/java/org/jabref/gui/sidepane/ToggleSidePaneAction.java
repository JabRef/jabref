package org.jabref.gui.sidepane;

import java.util.ArrayList;
import java.util.List;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.frame.SidePanePreferences;

import org.jspecify.annotations.NullMarked;

/// Hides the whole side pane, remembering which components were visible so it can be
/// restored later in the same session. Unlike [TogglePaneAction], this acts on the
/// entire pane rather than a single [SidePaneType].
@NullMarked
public class ToggleSidePaneAction extends SimpleCommand {
    private final StateManager stateManager;
    private final SidePanePreferences sidePanePreferences;

    // Session-only memory of what was visible before the pane was hidden.
    private List<SidePaneType> lastVisibleComponents = List.of();

    public ToggleSidePaneAction(StateManager stateManager, SidePanePreferences sidePanePreferences) {
        this.stateManager = stateManager;
        this.sidePanePreferences = sidePanePreferences;
    }

    @Override
    public void execute() {
        List<SidePaneType> visibleComponents = stateManager.getVisibleSidePaneComponents();

        if (!visibleComponents.isEmpty()) {
            lastVisibleComponents = new ArrayList<>(visibleComponents);
            visibleComponents.clear();
        } else if (!lastVisibleComponents.isEmpty()) {
            visibleComponents.addAll(lastVisibleComponents);
            visibleComponents.sort(new SidePaneViewModel.PreferredIndexSort(sidePanePreferences));
        } else {
            visibleComponents.addAll(sidePanePreferences.visiblePanes());
            visibleComponents.sort(new SidePaneViewModel.PreferredIndexSort(sidePanePreferences));
        }
    }
}
