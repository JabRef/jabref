package org.jabref.gui.entryeditor;

import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class PreviewSwitchAction extends SimpleCommand {

    public enum Direction { PREVIOUS, NEXT }

    private final LibraryTabContainer tabContainer;
    private final Direction direction;

    public PreviewSwitchAction(Direction direction, LibraryTabContainer tabContainer, StateManager stateManager) {
        this.tabContainer = tabContainer;
        this.direction = direction;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        if (direction == Direction.NEXT) {
            tabContainer.getCurrentLibraryTab().getEntryEditor().nextPreviewStyle();
        } else {
            tabContainer.getCurrentLibraryTab().getEntryEditor().previousPreviewStyle();
        }
    }
}
