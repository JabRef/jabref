package org.jabref.gui.exporter;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.exporter.ExportCustomizationDialogView;

public class ManageCustomExportsAction extends SimpleCommand {

    @Override
    public void execute() {
        new ExportCustomizationDialogView().show();
    }

}
