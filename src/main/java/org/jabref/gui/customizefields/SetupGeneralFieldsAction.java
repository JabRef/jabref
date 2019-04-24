package org.jabref.gui.customizefields;

import org.jabref.gui.actions.SimpleCommand;

public class SetupGeneralFieldsAction extends SimpleCommand {

    @Override
    public void execute() {
        new CustomizeGeneralFieldsDialogView().showAndWait();
    }
}
