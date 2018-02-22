package org.jabref.gui.actions;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.exporter.ExportCustomizationDialog;

public class ManageCustomExportsAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;

    public ManageCustomExportsAction(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Override
    public void execute() {
        ExportCustomizationDialog ecd = new ExportCustomizationDialog(jabRefFrame);
        ecd.setVisible(true);
    }

}
