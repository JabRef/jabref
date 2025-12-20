package org.jabref.model.util;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for monitoring directory changes.
 * Implementations watch directories for file system changes and notify listeners.
 */
public interface DirectoryUpdateMonitor {

    /**
     * Starts monitoring a directory for changes.
     *
     * @param directory The directory to monitor
     * @param listener  The listener to notify of changes
     * @param recursive Whether to monitor subdirectories recursively
     * @throws IOException if the directory cannot be monitored
     */
    void addListenerForDirectory(Path directory, DirectoryUpdateListener listener, boolean recursive) throws IOException;

    /**
     * Stops monitoring a directory.
     *
     * @param directory The directory to stop monitoring
     * @param listener  The listener to remove
     */
    void removeListener(Path directory, DirectoryUpdateListener listener);

    /**
     * Checks if the monitor is active.
     *
     * @return true if the monitor is running
     */
    boolean isActive();

    /**
     * Shuts down the monitor and releases resources.
     */
    void shutdown();
}
