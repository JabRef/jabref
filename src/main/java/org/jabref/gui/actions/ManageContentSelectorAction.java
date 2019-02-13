package org.jabref.gui.actions;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.contentselector.ContentSelectorDialogView;

public class ManageContentSelectorAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;

    public ManageContentSelectorAction(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Override
    public void execute() {
        new ContentSelectorDialogView(jabRefFrame).show();
    }
}
