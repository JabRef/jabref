package org.jabref.gui;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.database.BibDatabase;

public class ReplaceStringAction extends SimpleCommand
{
    private BasePanel basePanel;

    public ReplaceStringAction(BasePanel bPanel) {
        this.basePanel = bPanel;
    }

    @Override
    public void execute() {
        BibDatabase database = basePanel.getDatabase();
        ReplaceStringView rsc = new ReplaceStringView(database, basePanel);
        rsc.showAndWait().filter(response -> rsc.isExit());
    }
}
