package org.jabref.gui.bibtexextractor;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class ExtractBibtexAction extends SimpleCommand {

    public ExtractBibtexAction(StateManager stateManager) {
        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        ExtractBibtexDialog dlg = new ExtractBibtexDialog();
        dlg.showAndWait();
    }
}
