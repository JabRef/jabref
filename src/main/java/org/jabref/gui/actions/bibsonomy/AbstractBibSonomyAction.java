package org.jabref.gui.actions.bibsonomy;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.worker.AbstractWorker;

/**
 * This is the base class for all actions.
 * Provides a method to run workers asynchronously.
 */
public abstract class AbstractBibSonomyAction extends AbstractAction {

    private static final Log LOGGER = LogFactory.getLog(AbstractAction.class);

    private JabRefFrame jabRefFrame;

    public AbstractBibSonomyAction(JabRefFrame jabRefFrame, String text, Icon icon) {
        super(text, icon);
        this.jabRefFrame = jabRefFrame;
    }

    /**
     * Creates a action without text and icon
     */
    public AbstractBibSonomyAction(JabRefFrame jabRefFrame) {
        super();
        this.jabRefFrame = jabRefFrame;
    }

    /**
     * Runs a worker asynchronously. Includes catching exceptions and logging them
     *
     * @param worker the worker to be run asynchronously
     */
    protected void performAsynchronously(AbstractWorker worker) {
        try {
            BasePanel.runWorker(worker);
        } catch (Exception e) {
            jabRefFrame.unblock();
            LOGGER.error("Failed to initialize Worker", e);
        }
    }

    protected JabRefFrame getJabRefFrame() {
        return jabRefFrame;
    }
}
