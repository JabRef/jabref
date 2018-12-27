package org.jabref.gui.actions;

import org.jabref.gui.keyboard.KeyBindingsDialogView;

public class CustomizeKeyBindingAction extends SimpleCommand {

    @Override
    public void execute() {
        new KeyBindingsDialogView().show();
    }

}
