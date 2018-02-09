package org.jabref.gui.actions;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.importer.ImportCustomizationDialog;

public class ManageCustomImportsAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;

    public ManageCustomImportsAction(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Override
    public void execute() {
        ImportCustomizationDialog ecd = new ImportCustomizationDialog(jabRefFrame);
        ecd.setVisible(true);

    }

}
