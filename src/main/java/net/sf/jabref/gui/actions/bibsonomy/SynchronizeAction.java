package net.sf.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.worker.bibsonomy.SynchronizationWorker;
import net.sf.jabref.logic.l10n.Localization;


/**
 * {@link SynchronizeAction} runs the {@link SynchronizationWorker}
 *
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 */
public class SynchronizeAction extends AbstractBibSonomyAction {

    public SynchronizeAction(JabRefFrame jabRefFrame) {
        super(jabRefFrame, Localization.lang("Synchronize"), new ImageIcon(SynchronizeAction.class.getResource("/images/bibsonomy/arrow-circle-double-135.png")));
    }

    public void actionPerformed(ActionEvent e) {
        SynchronizationWorker worker = new SynchronizationWorker(getJabRefFrame());
        performAsynchronously(worker);
    }

}
