package org.jabref.gui.exporter;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.Actions;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;

public class SaveAllAction extends SimpleCommand {

    private final JabRefFrame frame;

    public SaveAllAction(JabRefFrame frame) {
        this.frame = frame;
    }

    @Override
    public void execute() {
        frame.output(Localization.lang("Saving all libraries..."));
        for (BasePanel panel : frame.getBasePanelList()) {
            if (!panel.getBibDatabaseContext().getDatabasePath().isPresent()) {
                frame.showBasePanel(panel);

                // TODO: Ask for path
            }
            panel.runCommand(Actions.SAVE);
            // TODO: can we find out whether the save was actually done or not?
        }
        frame.output(Localization.lang("Save all finished."));
    }
}
