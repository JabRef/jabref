package org.jabref.gui.actions;

import org.jabref.gui.exporter.ExportCustomizationDialogView;

public class ManageCustomExportsAction extends SimpleCommand {

    @Override
    public void execute() {
        new ExportCustomizationDialogView().show();
    }

}
