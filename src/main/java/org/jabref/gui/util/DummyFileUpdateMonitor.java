package org.jabref.gui.util;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;

/**
 * This {@link FileUpdateMonitor} does nothing.
 * Normally, you want to use {@link DefaultFileUpdateMonitor} except if you don't care about updates.
 */
public class DummyFileUpdateMonitor implements FileUpdateMonitor {
    @Override
    public void addListenerForFile(Path file, FileUpdateListener listener) throws IOException {

    }

    @Override
    public void removeListener(Path path, FileUpdateListener listener) {

    }
}
