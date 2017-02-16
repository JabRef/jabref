package net.sf.jabref.gui.util.bibsonomy;

import org.jabref.gui.worker.AbstractWorker;

public class WorkerUtil {

    /**
     * @deprecated Duplicate code.
     *
     * Somehow, this should be done using JabRefExecutorService or other ways.     *
     * This is not that easy, as we play around with the EDT
     *
     * FIXME: This method does NOT execute asynchronously
     */
    @Deprecated
    public static void performAsynchronously(AbstractWorker worker) throws Throwable {
        worker.init();
        worker.getWorker().run();
        worker.getCallBack().update();
    }

}
