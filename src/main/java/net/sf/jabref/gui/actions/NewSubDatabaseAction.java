package net.sf.jabref.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.auximport.FromAuxDialog;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.Defaults;
import net.sf.jabref.model.database.BibDatabaseContext;

/**
 * The action concerned with generate a new (sub-)database from latex AUX file.
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
            Defaults defaults = new Defaults(Globals.prefs.getDefaultBibDatabaseMode());
            BasePanel bp = new BasePanel(jabRefFrame, new BibDatabaseContext(dialog.getGenerateDB(), defaults));
            jabRefFrame.addTab(bp, true);
            jabRefFrame.output(Localization.lang("New database created."));
        }
    }
}
