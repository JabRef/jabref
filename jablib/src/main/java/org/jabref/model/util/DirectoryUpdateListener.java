package org.jabref.model.util;

import java.nio.file.Path;

/**
 * Listener interface for directory update events.
 * Implementations are notified when files or subdirectories are created, modified, or deleted.
 */
public interface DirectoryUpdateListener {

    /**
     * Called when a file is created in the monitored directory.
     *
     * @param path The path to the created file
     */
    void fileCreated(Path path);

    /**
     * Called when a file is modified in the monitored directory.
     *
     * @param path The path to the modified file
     */
    void fileModified(Path path);

    /**
     * Called when a file is deleted from the monitored directory.
     *
     * @param path The path to the deleted file
     */
    void fileDeleted(Path path);

    /**
     * Called when a subdirectory is created in the monitored directory.
     *
     * @param path The path to the created directory
     */
    void directoryCreated(Path path);

    /**
     * Called when a subdirectory is deleted from the monitored directory.
     *
     * @param path The path to the deleted directory
     */
    void directoryDeleted(Path path);
}
