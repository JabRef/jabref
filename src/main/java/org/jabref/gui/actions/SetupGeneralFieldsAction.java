package org.jabref.gui.actions;

import org.jabref.gui.genfields.GenFieldsCustomizerDialogView;

public class SetupGeneralFieldsAction extends SimpleCommand {

    @Override
    public void execute() {
        new GenFieldsCustomizerDialogView().show();

    }

}
