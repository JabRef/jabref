package org.jabref.gui.strings;

import org.jabref.gui.actions.SimpleCommand;

public class StringAction extends SimpleCommand {

    @Override
    public void execute() {
        new StringDialogView().show();
    }

}
