package org.jabref.model.util;

import java.io.IOException;
import java.nio.file.Path;

public interface DirectoryUpdateListener {

    /**
     * The directory has been updated. A new call will not result until the directory has been modified again.
     */
    void directoryRenamed(Path newPath);

    void directoryCreated(Path newPath) throws IOException;

    void fileUpdated();
}
