package org.jabref.gui.util;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Optional;

import org.jabref.JabRefException;
import org.jabref.model.util.FileUpdateListener;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.model.util.WatchServiceUnavailableException;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class monitors a set of files for changes. Upon detecting a change it notifies the registered {@link
 * FileUpdateListener}s.
 * <p>
 * Implementation based on https://stackoverflow.com/questions/16251273/can-i-watch-for-single-file-change-with-watchservice-not-the-whole-directory
 */
public class DefaultFileUpdateMonitor implements Runnable, FileUpdateMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFileUpdateMonitor.class);

    private final Multimap<Path, FileUpdateListener> listeners = ArrayListMultimap.create(20, 4);
    private WatchService watcher;
    private Optional<JabRefException> filesystemMonitorFailure;

    @Override
    public void run() {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            this.watcher = watcher;
            filesystemMonitorFailure = Optional.empty();

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
        } catch (IOException e) {
            filesystemMonitorFailure = Optional.of(new WatchServiceUnavailableException(e.getMessage(),
                    e.getLocalizedMessage(), e.getCause()));
            LOGGER.warn(filesystemMonitorFailure.get().getLocalizedMessage(), e);
        }
    }

    public boolean isActive() {
        return filesystemMonitorFailure.isEmpty();
    }

    private void notifyAboutChange(Path path) {
        listeners.get(path).forEach(FileUpdateListener::fileUpdated);
    }

    @Override
    public void addListenerForFile(Path file, FileUpdateListener listener) throws IOException {
        if (isActive()) {
            // We can't watch files directly, so monitor their parent directory for updates
            Path directory = file.toAbsolutePath().getParent();
            directory.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            listeners.put(file, listener);
        }
    }

    @Override
    public void removeListener(Path path, FileUpdateListener listener) {
        listeners.remove(path, listener);
    }
}
