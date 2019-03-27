package org.jabref.gui.actions;

import org.jabref.gui.importer.ImportCustomizationDialog;

public class ManageCustomImportsAction extends SimpleCommand {

    public ManageCustomImportsAction() {
    }

    @Override
    public void execute() {
        new ImportCustomizationDialog().showAndWait();
    }

}
