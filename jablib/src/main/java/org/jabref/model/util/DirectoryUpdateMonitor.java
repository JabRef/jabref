package org.jabref.model.util;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for monitoring directory changes.
 * Implementations watch directories for file system changes and notify listeners.
 * <p>
 * This interface is part of the DirectoryGroup feature infrastructure.
 * Currently used by {@link org.jabref.logic.util.DefaultDirectoryUpdateMonitor}.
 * Future integration will enable real-time updates when files are added/removed from monitored directories.
 * </p>
 */
public interface DirectoryUpdateMonitor {

    /**
     * Starts monitoring a directory for changes.
     * <p>
     * This method is called by DirectoryGroup when real-time monitoring is enabled.
     * Currently, DirectoryGroup uses static scanning at creation time, but this infrastructure
     * is prepared for future integration with WatchService for automatic group updates.
     * </p>
     *
     * @param directory The directory to monitor for file system changes
     * @param listener  The listener to notify when files/directories are created, modified, or deleted
     * @param recursive Whether to monitor subdirectories recursively (true for full tree monitoring)
     * @throws IOException if the directory cannot be registered with the WatchService
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
