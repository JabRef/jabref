package org.jabref.gui.customizefields;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.customizefields.CustomizeGeneralFieldsDialogView;

public class SetupGeneralFieldsAction extends SimpleCommand {

    @Override
    public void execute() {
        new CustomizeGeneralFieldsDialogView().showAndWait();
    }
}
