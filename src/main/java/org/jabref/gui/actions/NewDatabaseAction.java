package org.jabref.gui.actions;

import org.jabref.gui.JabRefFrame;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.Defaults;
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
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new Defaults(BibDatabaseMode.BIBTEX));
        bibDatabaseContext.setMode(mode);
        jabRefFrame.addTab(bibDatabaseContext, true);
        jabRefFrame.output(Localization.lang("New %0 library created.", mode.getFormattedName()));
    }
}
