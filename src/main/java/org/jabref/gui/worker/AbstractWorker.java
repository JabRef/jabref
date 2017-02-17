package org.jabref.gui.worker;

import spin.Spin;

/**
 * Convenience class for creating an object used for performing a time-
 * consuming action off the Swing thread, and optionally performing GUI
 * work afterwards. This class is supported by runCommand() in BasePanel,
 * which, if the action called is an AbstractWorker, will run its run()
 * method through the Worker interface, and then its update() method through
 * the CallBack interface. This procedure ensures that run() cannot freeze
 * the GUI, and that update() can safely update GUI components.
 */
public abstract class AbstractWorker implements Runnable, CallBack {

    private final Runnable worker;
    private final CallBack callBack;


    public AbstractWorker() {
        worker = (Runnable) Spin.off(this);
        callBack = (CallBack) Spin.over(this);

    }

    public void init() throws Exception {
        // Do nothing
    }

    /**
     * This method returns a wrapped Worker instance of this AbstractWorker.
     * whose methods will automatically be run off the EDT (Swing) thread.
     */
    public Runnable getWorker() {
        return worker;
    }

    /**
     * This method returns a wrapped CallBack instance of this AbstractWorker
     * whose methods will automatically be run on the EDT (Swing) thread.
     */
    public CallBack getCallBack() {
        return callBack;
    }

    /**
     * Empty implementation of the update() method. Override this method
     * if a callback is needed.
     */
    @Override
    public void update() {
        // Do nothing, see above
    }
}
