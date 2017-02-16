package net.sf.jabref.gui.util.bibsonomy;

import net.sf.jabref.gui.worker.bibsonomy.AbstractBibSonomyWorker;

import org.jabref.gui.importer.ImportInspectionDialog;

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
