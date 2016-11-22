package org.bibsonomy.plugin.jabref.util;

import net.sf.jabref.gui.importer.ImportInspectionDialog;

import org.bibsonomy.plugin.jabref.worker.AbstractBibSonomyWorker;

/**
 * {@link BibSonomyCallBack} is a util to stop execution of workers
 *
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
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
