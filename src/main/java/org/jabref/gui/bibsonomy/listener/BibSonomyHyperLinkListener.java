package org.jabref.gui.bibsonomy.listener;

import java.util.StringTokenizer;

import javax.swing.JComboBox;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.bibsonomy.GroupingComboBoxItem;
import org.jabref.gui.bibsonomy.SearchType;
import org.jabref.gui.worker.bibsonomy.ImportPostsByCriteriaWorker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Runs the {@link ImportPostsByCriteriaWorker} as soon as
 * the user clicks on a hyperlink in the tag cloud
 */
public class BibSonomyHyperLinkListener implements HyperlinkListener {

    private static final Log LOGGER = LogFactory.getLog(BibSonomyHyperLinkListener.class);


    private final JabRefFrame jabRefFrame;
    private final JComboBox<?> visibilityComboBox;

    public BibSonomyHyperLinkListener(JabRefFrame jabRefFrame, JComboBox<?> visibilityComboBox) {
        this.jabRefFrame = jabRefFrame;
        this.visibilityComboBox = visibilityComboBox;
    }

    @Override
	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType() == EventType.ACTIVATED) {
			StringTokenizer tokenizer = new StringTokenizer(e.getDescription(), " ");
			if (tokenizer.hasMoreElements()) {
				String criteria = tokenizer.nextToken();
                ImportPostsByCriteriaWorker worker = new ImportPostsByCriteriaWorker(
                        this.jabRefFrame,
                        criteria,
                        SearchType.TAGS,
                        ((GroupingComboBoxItem) visibilityComboBox.getSelectedItem()).getKey(),
                        ((GroupingComboBoxItem) visibilityComboBox.getSelectedItem()).getValue(),
                        false);
                try {
                    BasePanel.runWorker(worker);
                } catch (Throwable t) {
                    jabRefFrame.unblock();
                    LOGGER.error("Failed to initialize Worker", t);
                }
			}

		}
	}

}
