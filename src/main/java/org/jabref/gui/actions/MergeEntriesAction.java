package org.jabref.gui.actions;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.mergeentries.MergeEntriesDialog;

public class MergeEntriesAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;

    public MergeEntriesAction(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Override
    public void execute() {
        MergeEntriesDialog dlg = new MergeEntriesDialog(jabRefFrame.getCurrentBasePanel(), jabRefFrame.getDialogService());
        dlg.setVisible(true);
    }

}
