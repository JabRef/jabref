package org.jabref.gui.documentviewer;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.preferences.PreferencesService;

import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

public class ShowDocumentViewerAction extends SimpleCommand {

    public ShowDocumentViewerAction(StateManager stateManager, PreferencesService preferences) {
        this.executable.bind(needsEntriesSelected(stateManager).and(ActionHelper.isFilePresentForSelectedEntry(stateManager, preferences)));
    }

    @Override
    public void execute() {
        new DocumentViewerView().show();
    }
}
