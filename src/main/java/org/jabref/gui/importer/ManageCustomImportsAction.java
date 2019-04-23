package org.jabref.gui.importer;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.importer.ImportCustomizationDialog;

public class ManageCustomImportsAction extends SimpleCommand {

    public ManageCustomImportsAction() {
    }

    @Override
    public void execute() {
        new ImportCustomizationDialog().showAndWait();
    }

}
