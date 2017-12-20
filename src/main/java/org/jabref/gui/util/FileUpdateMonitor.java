package org.jabref.gui.util;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class monitors a set of files for changes. Upon detecting a change it notifies the registered {@link
 * FileUpdateListener}s.
 *
 * Implementation based on https://stackoverflow.com/questions/16251273/can-i-watch-for-single-file-change-with-watchservice-not-the-whole-directory
 */
public class FileUpdateMonitor implements Runnable {
    private static final Log LOGGER = LogFactory.getLog(FileUpdateMonitor.class);

    private final Multimap<Path, FileUpdateListener> listeners = ArrayListMultimap.create(20, 4);
    private WatchService watcher;

    @Override
    public void run() {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            this.watcher = watcher;
            while (true) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException e) {
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        Thread.yield();
                        continue;
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        // We only handle "ENTRY_MODIFY" here, so the context is always a Path
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path path = ((Path) key.watchable()).resolve(ev.context());
                        notifyAboutChange(path);
                    }
                    key.reset();
                }
                Thread.yield();
            }
        } catch (Throwable e) {
            LOGGER.debug("FileUpdateMonitor has been interrupted. Terminating...", e);
        }
    }

    private void notifyAboutChange(Path path) {
        listeners.get(path).forEach(FileUpdateListener::fileUpdated);
    }

    /**
     * Add a new file to monitor.
     *
     * @param file The file to monitor.
     * @throws IOException if the file does not exist.
     */
    public void addListenerForFile(Path file, FileUpdateListener listener) throws IOException {
        // We can't watch files directly, so monitor their parent directory for updates
        Path directory = file.toAbsolutePath().getParent();
        directory.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);

        listeners.put(file, listener);
    }

    /**
     * Removes a listener from the monitor.
     *
     * @param path The path to remove.
     */
    public void removeListener(Path path, FileUpdateListener listener) {
        listeners.remove(path, listener);
    }
}
