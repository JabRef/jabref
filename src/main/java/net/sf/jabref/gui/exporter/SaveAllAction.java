package net.sf.jabref.gui.exporter;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.Actions;
import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.worker.Worker;
import net.sf.jabref.logic.l10n.Localization;

import spin.Spin;

/**
 *
 * @author alver
 */
public class SaveAllAction extends MnemonicAwareAction implements Worker {

    private final JabRefFrame frame;
    private int databases;


    /** Creates a new instance of SaveAllAction */
    public SaveAllAction(JabRefFrame frame) {
        super(IconTheme.JabRefIcon.SAVE_ALL.getIcon());
        this.frame = frame;
        putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(KeyBinding.SAVE_ALL));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("Save all open databases"));
        putValue(Action.NAME, Localization.menuTitle("Save all"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        databases = frame.getBasePanelCount();
        frame.output(Localization.lang("Saving all databases..."));
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
