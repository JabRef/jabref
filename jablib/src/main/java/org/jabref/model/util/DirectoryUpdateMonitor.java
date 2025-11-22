package org.jabref.model.util;

import java.io.IOException;
import java.nio.file.Path;

public interface DirectoryUpdateMonitor {
    /**
     * Add a new directory to monitor.
     *
     * @param directory The directory to monitor.
     * @throws IOException if the directory does not exist.
     */
    void addListenerForDirectory(Path directory, DirectoryUpdateListener listener) throws IOException;

    /**
     * Removes a listener from the monitor.
     *
     * @param path The path to remove.
     */
    void removeListener(Path path, DirectoryUpdateListener listener);

    /**
     * Indicates whether the native system's directory monitor has successfully started.
     *
     * @return true if process is running; false otherwise.
     */
    boolean isActive();

    /**
     * stops watching for changes
     */
    void shutdown();
}
