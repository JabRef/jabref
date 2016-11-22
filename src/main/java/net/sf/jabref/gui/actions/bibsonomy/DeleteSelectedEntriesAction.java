package net.sf.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;

import javax.swing.ImageIcon;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import net.sf.jabref.gui.worker.bibsonomy.DeletePostsWorker;

/**
 * {@link DeleteSelectedEntriesAction} runs the {@link DeletePostsWorker}.
 *
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 */
public class DeleteSelectedEntriesAction extends AbstractBibSonomyAction {

    public void actionPerformed(ActionEvent e) {
        DeletePostsWorker worker = new DeletePostsWorker(getJabRefFrame(), getJabRefFrame().getCurrentBasePanel().getSelectedEntries().toArray(new BibEntry[0]));
        performAsynchronously(worker);
    }

    public DeleteSelectedEntriesAction(JabRefFrame jabRefFrame) {
        super(jabRefFrame, Localization.lang("Delete selected entries"), new ImageIcon(DeleteSelectedEntriesAction.class.getResource("/images/bibsonomy/document--minus.png")));
    }
}
