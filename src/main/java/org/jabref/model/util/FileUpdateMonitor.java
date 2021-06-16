package org.jabref.model.util;

import java.io.IOException;
import java.nio.file.Path;

public interface FileUpdateMonitor {
    /**
     * Add a new file to monitor.
     *
     * @param file The file to monitor.
     * @throws IOException if the file does not exist.
     */
    void addListenerForFile(Path file, FileUpdateListener listener) throws IOException;

    /**
     * Removes a listener from the monitor.
     *
     * @param path The path to remove.
     */
    void removeListener(Path path, FileUpdateListener listener);

    /**
     * Indicates whether or not the native system's file monitor has successfully started.
     * @return true is process is running; false otherwise.
     */
    boolean isActive();

    /**
     *  stops watching for changes
     */
    void shutdown();
}
