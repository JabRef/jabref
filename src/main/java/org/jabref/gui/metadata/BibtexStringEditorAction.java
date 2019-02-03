package org.jabref.gui.metadata;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.database.BibDatabase;

public class BibtexStringEditorAction extends SimpleCommand {

    private final JabRefFrame frame;

    public BibtexStringEditorAction(JabRefFrame jabRefFrame) {
        this.frame = jabRefFrame;
    }

    @Override
    public void execute() {
        BibDatabase database = frame.getCurrentBasePanel().getDatabase();
        new BibtexStringEditorDialogView(database).showAndWait();
    }
}
