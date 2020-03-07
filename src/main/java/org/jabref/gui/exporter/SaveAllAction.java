package org.jabref.gui.exporter;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;

public class SaveAllAction extends SimpleCommand {

    private final JabRefFrame frame;
    private final DialogService dialogService;

    public SaveAllAction(JabRefFrame frame) {
        this.frame = frame;
        this.dialogService = frame.getDialogService();
    }

    @Override
    public void execute() {
        dialogService.notify(Localization.lang("Saving all libraries..."));

        for (BasePanel panel : frame.getBasePanelList()) {
            SaveDatabaseAction saveDatabaseAction = new SaveDatabaseAction(panel, Globals.prefs, Globals.entryTypesManager);
            if (panel.getBibDatabaseContext().getDatabasePath().isEmpty()) {
                //It will ask a path before saving.
                saveDatabaseAction.saveAs();
            } else {
                saveDatabaseAction.save();
                // TODO: can we find out whether the save was actually done or not?
            }
        }

        dialogService.notify(Localization.lang("Save all finished."));
    }
}
