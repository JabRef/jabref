package org.jabref.gui.actions;

import javax.swing.SwingUtilities;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.BasePanelPreferences;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.auximport.FromAuxDialog;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.Defaults;
import org.jabref.model.database.BibDatabaseContext;

/**
 * The action concerned with generate a new (sub-)database from latex AUX file.
 */
public class NewSubLibraryAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;

    public NewSubLibraryAction(JabRefFrame jabRefFrame) {
        this.jabRefFrame = jabRefFrame;
    }

    @Override
    public void execute() {
        SwingUtilities.invokeLater(() -> {
            FromAuxDialog dialog = new FromAuxDialog(jabRefFrame, "", true, jabRefFrame.getTabbedPane());

            dialog.setVisible(true);

            if (dialog.generatePressed()) {
                Defaults defaults = new Defaults(Globals.prefs.getDefaultBibDatabaseMode());
                BasePanel bp = new BasePanel(jabRefFrame, BasePanelPreferences.from(Globals.prefs), new BibDatabaseContext(dialog.getGenerateDB(), defaults), ExternalFileTypes.getInstance());
                jabRefFrame.addTab(bp, true);
                jabRefFrame.output(Localization.lang("New library created."));
            }
        });
    }

}
