package org.jabref.gui.exporter;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.Actions;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;

public class SaveAllAction extends SimpleCommand implements Runnable {

    private final JabRefFrame frame;
    private int databases;


    /** Creates a new instance of SaveAllAction */
    public SaveAllAction(JabRefFrame frame) {
        this.frame = frame;
    }

    @Override
    public void execute() {
        databases = frame.getBasePanelCount();
        frame.output(Localization.lang("Saving all libraries..."));
        run();
        frame.output(Localization.lang("Save all finished."));
    }

    @Override
    public void run() {
        for (int i = 0; i < databases; i++) {
            if (i < frame.getBasePanelCount()) {
                BasePanel panel = frame.getBasePanelAt(i);
                if (!panel.getBibDatabaseContext().getDatabaseFile().isPresent()) {
                    frame.showBasePanelAt(i);
                }
                panel.runCommand(Actions.SAVE);
                // TODO: can we find out whether the save was actually done or not?
            }
        }
    }

}
