package org.jabref.gui.contentselector;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class ManageContentSelectorAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;

    public ManageContentSelectorAction(JabRefFrame jabRefFrame, StateManager stateManager) {
        this.jabRefFrame = jabRefFrame;

        this.executable.bind(needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        BasePanel basePanel = jabRefFrame.getCurrentBasePanel();
        new ContentSelectorDialogView(basePanel).showAndWait();
    }
}
