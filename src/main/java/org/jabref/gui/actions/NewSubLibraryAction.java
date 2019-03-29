package org.jabref.gui.actions;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.auximport.FromAuxDialog;

/**
 * The action concerned with generate a new (sub-)database from latex AUX file.
 */
public class NewSubLibraryAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;

    public NewSubLibraryAction(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Override
    public void execute() {
        FromAuxDialog dialog = new FromAuxDialog(jabRefFrame);
        dialog.showAndWait();
    }
}
