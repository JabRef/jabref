package org.jabref.gui.util.bibsonomy;

import org.jabref.gui.importer.ImportInspectionDialog;
import org.jabref.gui.worker.bibsonomy.AbstractBibSonomyWorker;

/**
 * Is a util to stop execution of workers
 */
public class BibSonomyCallBack implements ImportInspectionDialog.CallBack {

	private AbstractBibSonomyWorker worker;

	public void stopFetching() {
		if (worker != null)
			worker.stopFetching();
	}

	public BibSonomyCallBack(AbstractBibSonomyWorker pluginWorker) {
		this.worker = pluginWorker;
	}
}
