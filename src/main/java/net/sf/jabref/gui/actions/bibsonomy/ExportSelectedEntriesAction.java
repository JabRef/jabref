package net.sf.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.util.bibsonomy.CheckTagsUtil;
import net.sf.jabref.gui.worker.bibsonomy.ExportWorker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * {@link ExportSelectedEntriesAction} checks for entries without keywords and does
 * an export of all selected entries to the service.
 *
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 */
public class ExportSelectedEntriesAction extends AbstractBibSonomyAction {

    private static final Log LOGGER = LogFactory.getLog(ExportSelectedEntriesAction.class);

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
        super(jabRefFrame, Localization.lang("Export selected entries"), new ImageIcon(ExportSelectedEntriesAction.class.getResource("/images/bibsonomy/document--arrow.png")));
    }
}