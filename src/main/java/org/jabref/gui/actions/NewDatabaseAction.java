package org.jabref.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.Defaults;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

/**
 * The action concerned with opening a new database.
 */
public class NewDatabaseAction extends MnemonicAwareAction {

    private final JabRefFrame jabRefFrame;
    private final BibDatabaseMode mode;

    public NewDatabaseAction(JabRefFrame jabRefFrame, BibDatabaseMode mode) {
        super(IconTheme.JabRefIcon.NEW.getIcon());
        this.jabRefFrame = jabRefFrame;
        this.mode = mode;
        putValue(Action.NAME, Localization.menuTitle("New %0 library", mode.getFormattedName()));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("New %0 library", mode.getFormattedName()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Create a new, empty, database.
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new Defaults(BibDatabaseMode.BIBTEX));
        bibDatabaseContext.setMode(mode);
        jabRefFrame.addTab(bibDatabaseContext, true);
        jabRefFrame.output(Localization.lang("New %0 library created.", mode.getFormattedName()));
    }
}
