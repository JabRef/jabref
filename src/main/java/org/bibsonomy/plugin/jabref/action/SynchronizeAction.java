package org.bibsonomy.plugin.jabref.action;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;

import org.bibsonomy.plugin.jabref.worker.SynchronizationWorker;


/**
 * {@link SynchronizeAction} runs the {@link SynchronizationWorker}
 *
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 */
public class SynchronizeAction extends AbstractBibSonomyAction {

    public void actionPerformed(ActionEvent e) {
        SynchronizationWorker worker = new SynchronizationWorker(getJabRefFrame());
        performAsynchronously(worker);
    }

    public SynchronizeAction(JabRefFrame jabRefFrame) {
        super(jabRefFrame, Localization.lang("Synchronize"), new ImageIcon(SynchronizeAction.class.getResource("/images/images/arrow-circle-double-135.png")));

    }
}
