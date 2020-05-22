package org.jabref.gui.importer;

import org.jabref.gui.actions.SimpleCommand;

public class ManageCustomImportsAction extends SimpleCommand {

    public ManageCustomImportsAction() {
    }

    @Override
    public void execute() {
        new ImportCustomizationDialog().showAndWait();
    }
}
