package org.jabref.gui.documentviewer;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.preferences.PreferencesService;

public class ShowDocumentViewerAction extends SimpleCommand {

    public ShowDocumentViewerAction(StateManager stateManager, PreferencesService preferences) {
        this.executable.bind(ActionHelper.isFilePresentForSelectedEntry(stateManager, preferences));
    }

    @Override
    public void execute() {
        new DocumentViewerView().show();
    }
}
