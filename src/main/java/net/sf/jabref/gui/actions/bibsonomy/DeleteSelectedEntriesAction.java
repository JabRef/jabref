package net.sf.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.worker.bibsonomy.DeletePostsWorker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

/**
 * Runs the {@link DeletePostsWorker}.
 */
public class DeleteSelectedEntriesAction extends AbstractBibSonomyAction {

    public void actionPerformed(ActionEvent e) {
        DeletePostsWorker worker = new DeletePostsWorker(getJabRefFrame(), getJabRefFrame().getCurrentBasePanel().getSelectedEntries().toArray(new BibEntry[0]));
        performAsynchronously(worker);
    }

    public DeleteSelectedEntriesAction(JabRefFrame jabRefFrame) {
        super(jabRefFrame, Localization.lang("Delete selected entries"), IconTheme.JabRefIcon.DELETE_ENTRY.getIcon());
    }
}
