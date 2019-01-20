package org.jabref.gui.actions;

import org.jabref.gui.customizefields.CustomizeGeneralFieldsDialogView;

public class SetupGeneralFieldsAction extends SimpleCommand {

    @Override
    public void execute() {
        new CustomizeGeneralFieldsDialogView().show();

    }

}
