package org.jabref.gui.customentrytypes;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;

public class CustomizeEntryAction extends SimpleCommand {

    private final JabRefFrame frame;

    public CustomizeEntryAction(JabRefFrame frame) {
        this.frame = frame;
    }

    @Override
    public void execute() {
        CustomizeEntryTypeDialogView dialog = new CustomizeEntryTypeDialogView(this.frame.getCurrentBasePanel().getBibDatabaseContext());
        dialog.showAndWait();
    }
}
