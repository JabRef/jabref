package org.jabref.model.util;

public interface FileUpdateListener {

    /**
     * The file has been updated. A new call will not result until the file has been modified again.
     */
    void fileUpdated();
}
