package org.jabref.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.auximport.FromAuxDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.Defaults;
import org.jabref.model.database.BibDatabaseContext;

/**
 * The action concerned with generate a new (sub-)database from latex AUX file.
 */
public class NewSubDatabaseAction extends MnemonicAwareAction {

    private final JabRefFrame jabRefFrame;

    public NewSubDatabaseAction(JabRefFrame jabRefFrame) {
        super(IconTheme.JabRefIcon.NEW.getIcon());
        this.jabRefFrame = jabRefFrame;
        putValue(Action.NAME, Localization.menuTitle("New sublibrary based on AUX file") + "...");
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("New BibTeX sublibrary"));
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
            jabRefFrame.output(Localization.lang("New library created."));
        }
    }
}
