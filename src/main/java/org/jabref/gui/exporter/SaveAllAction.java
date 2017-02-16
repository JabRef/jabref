package org.jabref.gui.exporter;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.Actions;
import org.jabref.gui.actions.MnemonicAwareAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;

import spin.Spin;

/**
 *
 * @author alver
 */
public class SaveAllAction extends MnemonicAwareAction implements Runnable {

    private final JabRefFrame frame;
    private int databases;


    /** Creates a new instance of SaveAllAction */
    public SaveAllAction(JabRefFrame frame) {
        super(IconTheme.JabRefIcon.SAVE_ALL.getIcon());
        this.frame = frame;
        putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(KeyBinding.SAVE_ALL));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Save all open libraries"));
        putValue(Action.NAME, Localization.menuTitle("Save all"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        databases = frame.getBasePanelCount();
        frame.output(Localization.lang("Saving all libraries..."));
        Spin.off(this);
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
