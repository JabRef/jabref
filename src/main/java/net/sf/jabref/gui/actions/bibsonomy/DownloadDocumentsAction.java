package net.sf.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.ImageIcon;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import net.sf.jabref.gui.worker.bibsonomy.DownloadDocumentsWorker;

/**
 * {@link DownloadDocumentsAction} collects all entries of a the {@link net.sf.jabref.model.database.BibDatabase BibDatabase} and fetches all documents available for the post.
 *
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 */
public class DownloadDocumentsAction extends AbstractBibSonomyAction {

    public DownloadDocumentsAction(JabRefFrame jabRefFrame) {
        super(jabRefFrame, Localization.lang("Download my documents"), new ImageIcon(DownloadDocumentsAction.class.getResource("/images/bibsonomy/document-pdf-text.png")));
    }

    public void actionPerformed(ActionEvent e) {
        if (getJabRefFrame().getCurrentBasePanel() != null) {
            Collection<BibEntry> entries = getJabRefFrame().getCurrentBasePanel().getDatabase().getEntries();
            for (BibEntry entry : entries) {
                DownloadDocumentsWorker worker = new DownloadDocumentsWorker(getJabRefFrame(), entry, false);
                performAsynchronously(worker);
            }
        }
    }
}
