package org.jabref.gui.bibsonomy.listener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.worker.bibsonomy.DownloadDocumentsWorker;
import org.jabref.model.database.event.EntryAddedEvent;
import org.jabref.model.entry.BibEntry;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * {@link BibSonomyDataBaseChangeListener} runs the {@link DownloadDocumentsWorker} as soon as a new entry was added to the database.
 */
public class BibSonomyDataBaseChangeListener {

	private static final Log LOGGER = LogFactory.getLog(BibSonomyDataBaseChangeListener.class);

	private final ExecutorService threadPool;

	private JabRefFrame jabRefFrame;

	public BibSonomyDataBaseChangeListener(JabRefFrame jabRefFrame) {
		this.jabRefFrame = jabRefFrame;
		threadPool = Executors.newSingleThreadExecutor();
	}

	@Subscribe
	public void listen(EntryAddedEvent event) {
		threadPool.execute(() -> {
			final BibEntry entry = event.getBibEntry();
			if (entry != null) {
				try {
					// sleep here because we implicitly rely on another
					// DatabaseChangeListener (the JabRef internal
					// GlazedEntrySorter) to have finished in advance.
					// Simply sleeping is not 100% safe also but there
					// seems to be no event to wait for this properly.
					Thread.sleep(200);
					DownloadDocumentsWorker worker = new DownloadDocumentsWorker(jabRefFrame, entry, true);
                    try {
                        BasePanel.runWorker(worker);
                    } catch (Exception e) {
                        jabRefFrame.unblock();
                        LOGGER.error("Failed to initialize Worker", e);
                    }
				} catch (InterruptedException e) {
					LOGGER.warn("Interrupt while downloading private documents", e);
					Thread.currentThread().interrupt();
				} catch (Exception e) {
					LOGGER.error("Error while downloading private documents", e);
				}
			}
		});
	}

}
