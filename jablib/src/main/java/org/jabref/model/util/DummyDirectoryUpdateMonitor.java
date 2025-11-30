package org.jabref.model.util;

import java.nio.file.Path;

/// This [DirectoryUpdateMonitor] does nothing.
/// Normally, you want to use `org.jabref.gui.util.DefaultDirectoryUpdateMonitor` except if you don't care about updates.
public class DummyDirectoryUpdateMonitor implements DirectoryUpdateMonitor {

    @Override
    public void addListenerForDirectory(Path directory, DirectoryUpdateListener listener) {
        // empty
    }

    @Override
    public void removeListener(Path path, DirectoryUpdateListener listener) {
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
