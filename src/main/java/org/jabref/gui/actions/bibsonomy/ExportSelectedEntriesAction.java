package org.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JOptionPane;

import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.util.bibsonomy.CheckTagsUtil;
import org.jabref.gui.worker.bibsonomy.ExportWorker;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

import org.jabref.gui.util.bibsonomy.CheckTagsUtil;
import org.jabref.gui.worker.bibsonomy.ExportWorker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Checks for entries without keywords and does
 * an export of all selected entries to the service.
 */
public class ExportSelectedEntriesAction extends AbstractBibSonomyAction {

    private static final Log LOGGER = LogFactory.getLog(ExportSelectedEntriesAction.class);

    @SuppressWarnings("FallThrough")
    public void actionPerformed(ActionEvent e) {

        List<BibEntry> entries = getJabRefFrame().getCurrentBasePanel().getSelectedEntries();

        CheckTagsUtil ctu = new CheckTagsUtil(entries, getJabRefFrame());
        switch (ctu.hasAPostNoTagsAssigned()) {
            case JOptionPane.YES_OPTION:
                // tags missing & user has explicitly chosen to add default tag
                ctu.assignDefaultTag();
            case JOptionPane.DEFAULT_OPTION:
                // this means that all posts have tags
                ExportWorker worker = new ExportWorker(getJabRefFrame(), entries);
                performAsynchronously(worker);
                break;
            default:
                // happens when tags are missing, and user wants to cancel export
                LOGGER.debug(Localization.lang("Selected post have no tags assigned"));
        }

    }

    public ExportSelectedEntriesAction(JabRefFrame jabRefFrame) {
        super(jabRefFrame, Localization.lang("Export selected entries"), IconTheme.JabRefIcon.EXPORT.getIcon());
    }
}
