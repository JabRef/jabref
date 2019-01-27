package org.jabref.gui.edit;

import org.jabref.gui.BasePanel;
import org.jabref.gui.actions.SimpleCommand;

public class ReplaceStringAction extends SimpleCommand
{
    private BasePanel basePanel;

    public ReplaceStringAction(BasePanel basePanel) {
        this.basePanel = basePanel;
    }

    @Override
    public void execute() {
        ReplaceStringView dialog = new ReplaceStringView(basePanel);
        dialog.showAndWait();
    }
}
