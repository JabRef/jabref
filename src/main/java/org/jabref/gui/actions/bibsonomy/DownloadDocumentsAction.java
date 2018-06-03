package org.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;
import java.util.Collection;

import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.worker.bibsonomy.DownloadDocumentsWorker;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

/**
 * Collects all entries of a the {@link org.jabref.model.database.BibDatabase BibDatabase} and fetches all documents available for the post.
 */
public class DownloadDocumentsAction extends AbstractBibSonomyAction {

    public DownloadDocumentsAction(JabRefFrame jabRefFrame) {
        super(jabRefFrame, Localization.lang("Download my documents"), IconTheme.JabRefIcon.CLOUD_DOWNLOAD.getIcon());
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
