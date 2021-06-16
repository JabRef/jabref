package org.jabref.model.util;

import java.nio.file.Path;

/**
 * This {@link FileUpdateMonitor} does nothing.
 * Normally, you want to use {@link org.jabref.gui.util.DefaultFileUpdateMonitor} except if you don't care about updates.
 */
public class DummyFileUpdateMonitor implements FileUpdateMonitor {

    @Override
    public void addListenerForFile(Path file, FileUpdateListener listener) {
        // empty
    }

    @Override
    public void removeListener(Path path, FileUpdateListener listener) {
        // empty
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public void shutdown() {
        // empty
    }
}
