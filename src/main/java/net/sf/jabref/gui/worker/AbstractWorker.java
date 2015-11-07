/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.worker;

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
public abstract class AbstractWorker implements Worker, CallBack {

    private final Worker worker;
    private final CallBack callBack;


    public AbstractWorker() {
        worker = (Worker) Spin.off(this);
        callBack = (CallBack) Spin.over(this);

    }

    public void init() throws Throwable {
        // Do nothing
    }

    /**
     * This method returns a wrapped Worker instance of this AbstractWorker.
     * whose methods will automatically be run off the EDT (Swing) thread.
     */
    public Worker getWorker() {
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
