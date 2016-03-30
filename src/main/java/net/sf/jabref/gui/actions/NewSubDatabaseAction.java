package net.sf.jabref.gui.actions;

import net.sf.jabref.Defaults;
import net.sf.jabref.Globals;
import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.*;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.gui.auximport.FromAuxDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * The action concerned with generate a new (sub-)database from latex aux file.
 */
public class NewSubDatabaseAction extends MnemonicAwareAction {

    private final JabRefFrame jabRefFrame;

    public NewSubDatabaseAction(JabRefFrame jabRefFrame) {
        super(IconTheme.JabRefIcon.NEW.getIcon());
        this.jabRefFrame = jabRefFrame;
        putValue(Action.NAME, Localization.menuTitle("New subdatabase based on AUX file") + "...");
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("New BibTeX subdatabase"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Create a new, empty, database.

        FromAuxDialog dialog = new FromAuxDialog(jabRefFrame, "", true, jabRefFrame.getTabbedPane());

        dialog.setLocationRelativeTo(jabRefFrame);
        dialog.setVisible(true);

        if (dialog.generatePressed()) {
            Defaults defaults = new Defaults(
                    BibDatabaseMode.fromPreference(Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE)));
            BasePanel bp = new BasePanel(jabRefFrame, new BibDatabaseContext(dialog.getGenerateDB(), defaults),
                    Globals.prefs.getDefaultEncoding()); // meta data
            jabRefFrame.addTab(bp, true);
            jabRefFrame.output(Localization.lang("New database created."));
        }
    }
}
