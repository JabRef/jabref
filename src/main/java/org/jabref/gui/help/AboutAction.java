package org.jabref.gui.help;

import org.jabref.gui.actions.SimpleCommand;

public class AboutAction extends SimpleCommand {

    @Override
    public void execute() {
        new AboutDialogView().show();
    }
}
