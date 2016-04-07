package net.sf.jabref.gui.actions;

import net.sf.jabref.Defaults;
import net.sf.jabref.Globals;
import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabaseMode;

import javax.swing.*;
import java.awt.event.ActionEvent;

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
        putValue(Action.NAME, Localization.menuTitle("New %0 database", mode.getFormattedName()));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("New %0 database", mode.getFormattedName()));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Create a new, empty, database.
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new Defaults(BibDatabaseMode.BIBTEX));
        bibDatabaseContext.setMode(mode);
        jabRefFrame.addTab(bibDatabaseContext, Globals.prefs.getDefaultEncoding(), true);
        jabRefFrame.output(Localization.lang("New %0 database created.", mode.getFormattedName()));
    }
}
