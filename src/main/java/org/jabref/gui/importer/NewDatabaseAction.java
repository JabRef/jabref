package org.jabref.gui.importer;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

/**
 * Create a new, empty, database.
 */
public class NewDatabaseAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;
    private final BibDatabaseMode mode;

    public NewDatabaseAction(JabRefFrame jabRefFrame, BibDatabaseMode mode) {
        this.jabRefFrame = jabRefFrame;
        this.mode = mode;
    }

    @Override
    public void execute() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext();
        bibDatabaseContext.setMode(mode);
        jabRefFrame.addTab(bibDatabaseContext, true);
    }
}
