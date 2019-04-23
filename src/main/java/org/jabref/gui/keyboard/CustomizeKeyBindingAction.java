package org.jabref.gui.keyboard;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.keyboard.KeyBindingsDialogView;

public class CustomizeKeyBindingAction extends SimpleCommand {

    @Override
    public void execute() {
        new KeyBindingsDialogView().show();
    }

}
