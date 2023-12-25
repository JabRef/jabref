package org.jabref.gui.entryeditor;

import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;

public class OpenEntryEditorAction extends SimpleCommand {

    private final LibraryTabContainer tabContainer;
    private final StateManager stateManager;

    public OpenEntryEditorAction(LibraryTabContainer tabContainer, StateManager stateManager) {
        this.tabContainer = tabContainer;
        this.stateManager = stateManager;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    public void execute() {
        if (!stateManager.getSelectedEntries().isEmpty()) {
            tabContainer.getCurrentLibraryTab().showAndEdit(stateManager.getSelectedEntries().getFirst());
        }
    }
}
