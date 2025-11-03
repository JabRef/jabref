package org.jabref.gui.util;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jabref.logic.JabRefException;
import org.jabref.logic.WatchServiceUnavailableException;
import org.jabref.model.util.DirectoryUpdateListener;
import org.jabref.model.util.DirectoryUpdateMonitor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class monitors a set of directories for changes. Upon detecting a change it notifies the registered {@link
 * DirectoryUpdateListener}s.
 * <p>
 */
public class DefaultDirectoryUpdateMonitor implements Runnable, DirectoryUpdateMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDirectoryUpdateMonitor.class);

    private final Multimap<Path, DirectoryUpdateListener> listeners = ArrayListMultimap.create(20, 4);
    private volatile WatchService watcher;
    private final AtomicBoolean notShutdown = new AtomicBoolean(true);
    private final AtomicReference<Optional<JabRefException>> filesystemMonitorFailure = new AtomicReference<>(Optional.empty());

    @Override
    public void run() {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            this.watcher = watcher;
            filesystemMonitorFailure.set(Optional.empty());

            while (notShutdown.get()) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException | ClosedWatchServiceException e) {
                    return;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        Thread.yield();
                        continue;
                    } else if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        // We only handle "ENTRY_CREATE" and "ENTRY_MODIFY" here, so the context is always a Path
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path path = ((Path) key.watchable()).resolve(ev.context());
                        if (Files.isDirectory(path)) {
                            notifyAboutDirectoryChange(path);
                        } else {
                            notifyAboutFileChange(path);
                        }
                    }
                    key.reset();
                }
                Thread.yield();
            }
        } catch (IOException e) {
            JabRefException exception = new WatchServiceUnavailableException(
                    e.getMessage(), e.getLocalizedMessage(), e.getCause());
            filesystemMonitorFailure.set(Optional.of(exception));
            LOGGER.warn("Error during watching", e);
        }
    }

    @Override
    public boolean isActive() {
        return filesystemMonitorFailure.get().isEmpty();
    }

    private void notifyAboutDirectoryChange(Path newPath) {
        // Either there is a new directory or a directory has been renamed: the path given is not linked with a listener
        Boolean directoryRenamed = false;
        List<DirectoryUpdateListener> oldListeners = new ArrayList<>();
        List<Path> oldPaths = new ArrayList<>();
        for (Path registeredPath : listeners.keys()) {
            if (!Files.exists(registeredPath)) {
                // The registeredPath is no longer linked to an existing directory: this is the old path of a renamed directory
                for (DirectoryUpdateListener listener : listeners.get(registeredPath)) {
                    oldListeners.add(listener);
                    oldPaths.add(registeredPath);
                    directoryRenamed = true;
                    System.out.println("Directory Renamed: " + registeredPath + " -> " + newPath);
                }
            }
        }
        if (directoryRenamed) {
            for (int i = 0; i < oldListeners.size(); i++) {
                listeners.remove(oldPaths.get(i), oldListeners.get(i));
                listeners.put(newPath, oldListeners.get(i));
                oldListeners.get(i).directoryRenamed(newPath);
            }
        } else {
            // This case occurs if there is a new directory within the structure
            Path parentPath = newPath.toAbsolutePath().getParent();
            for (DirectoryUpdateListener listener : listeners.get(parentPath)) {
                try {
                    listener.directoryCreated(newPath);
                } catch (IOException e) {
                    LOGGER.error("Error while creating directory", e);
                }
            }
            System.out.println("New directory: " + newPath);
        }
    }

    private void notifyAboutFileChange(Path path) {
        listeners.get(path).forEach(DirectoryUpdateListener::fileUpdated);
    }

    @Override
    public void addListenerForDirectory(Path directory, DirectoryUpdateListener listener) throws IOException {
        if (isActive()) {
            directory.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
            listeners.put(directory, listener);
        } else {
            LOGGER.warn("Not adding listener {} to file {} because the directory update monitor isn't active", listener, directory);
        }
    }

    @Override
    public void removeListener(Path path, DirectoryUpdateListener listener) {
        listeners.remove(path, listener);
    }

    @Override
    public void shutdown() {
        try {
            notShutdown.set(false);
            WatchService watcher = this.watcher;
            if (watcher != null) {
                watcher.close();
            }
        } catch (IOException e) {
            LOGGER.error("error closing watcher", e);
        }
    }
}
