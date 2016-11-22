package org.bibsonomy.plugin.jabref.action;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.worker.AbstractWorker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bibsonomy.plugin.jabref.util.WorkerUtil;

/**
 * {@link AbstractBibSonomyAction} is the base class for all actions.
 * Provides a method to run workers asynchronously.
 *
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 */
public abstract class AbstractBibSonomyAction extends AbstractAction {

    private static final Log LOGGER = LogFactory.getLog(SearchAction.class);

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
     * Runs a worker asynchronously. Includes exception handling.
     *
     * @param worker the worker to be run asynchronously
     */
    protected void performAsynchronously(AbstractWorker worker) {
        try {
            WorkerUtil.performAsynchronously(worker);
        } catch (Throwable t) {
            jabRefFrame.unblock();
            LOGGER.error("Failed to initialize Worker", t);
            t.printStackTrace();
        }
    }

    protected JabRefFrame getJabRefFrame() {
        return jabRefFrame;
    }
}
