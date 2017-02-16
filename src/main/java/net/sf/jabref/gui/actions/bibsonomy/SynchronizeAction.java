package net.sf.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.worker.bibsonomy.SynchronizationWorker;
import net.sf.jabref.logic.l10n.Localization;


/**
 * Runs the {@link SynchronizationWorker}
 */
public class SynchronizeAction extends AbstractBibSonomyAction {

    public SynchronizeAction(JabRefFrame jabRefFrame) {
        super(jabRefFrame, Localization.lang("Synchronize"), IconTheme.JabRefIcon.CLOUD_SYNC.getIcon());
    }

    public void actionPerformed(ActionEvent e) {
        SynchronizationWorker worker = new SynchronizationWorker(getJabRefFrame());
        performAsynchronously(worker);
    }

}
