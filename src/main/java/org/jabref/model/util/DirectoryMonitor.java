package org.jabref.model.util;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;

public interface DirectoryMonitor {
    /**
     * Add an observer to the monitor.
     *
     * @param observer The directory to observe.
     * @param listener The listener to invoke when the directory changes.
     */
    void addObserver(FileAlterationObserver observer, FileAlterationListener listener);

    /**
     * Remove an observer from the monitor.
     *
     * @param observer The directory to stop monitoring.
     */
    void removeObserver(FileAlterationObserver observer);

    void start();

    void shutdown();
}
