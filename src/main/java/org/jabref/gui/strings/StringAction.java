package org.jabref.gui.strings;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.database.BibDatabase;

public class StringAction extends SimpleCommand {

    private final JabRefFrame frame;

    public StringAction(JabRefFrame jabRefFrame) {
        this.frame = jabRefFrame;
    }

    @Override
    public void execute() {
        BibDatabase database = frame.getCurrentBasePanel().getDatabase();
        new StringDialogView(database).showAndWait();
    }

}
