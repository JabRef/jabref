package net.sf.jabref.gui.util.bibsonomy;

import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.gui.worker.CallBack;
import net.sf.jabref.gui.worker.Worker;

/**
 * {@link WorkerUtil} runs a worker asynchronouslys
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
 *
 */
public class WorkerUtil {

	/**
	 * @deprecated Duplicate code.
	 * See {@link net.sf.jabref.gui.exporter.SaveDatabaseAction}
	 */
	@Deprecated
	public static void performAsynchronously(AbstractWorker worker) throws Throwable {
		//TODO: Duplicate code. See SaveDatabaseAction - zellerdev
		Worker wrk = worker.getWorker();
		CallBack cb = worker.getCallBack();

		worker.init();

		wrk.run();

		cb.update();
	}
}