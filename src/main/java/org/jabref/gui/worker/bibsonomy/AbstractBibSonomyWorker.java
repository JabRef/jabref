package org.jabref.gui.worker.bibsonomy;

import java.util.Objects;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.worker.AbstractWorker;

/**
 * Is the base for all Workers which need to support stopping execution.
 */
public abstract class AbstractBibSonomyWorker extends AbstractWorker {

	private boolean fetchNext = true;
	protected final JabRefFrame jabRefFrame;

	public AbstractBibSonomyWorker(JabRefFrame jabRefFrame) {
		this.jabRefFrame = Objects.requireNonNull(jabRefFrame);
	}

	public synchronized void stopFetching() {
		fetchNext = false;
	}

	protected synchronized boolean fetchNext() {
		return fetchNext;
	}

}
