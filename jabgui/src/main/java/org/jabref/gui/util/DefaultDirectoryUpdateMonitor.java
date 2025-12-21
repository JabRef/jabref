package org.jabref.gui.util;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
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
 * Monitors directories for file system changes using Java's WatchService.
 * Notifies registered listeners when files or directories are created, modified, or deleted.
 */
public class DefaultDirectoryUpdateMonitor implements Runnable, DirectoryUpdateMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDirectoryUpdateMonitor.class);

    private final Multimap<Path, DirectoryUpdateListener> listeners = ArrayListMultimap.create(20, 4);
    private final Map<WatchKey, Path> watchKeyToPath = new HashMap<>();
    private volatile WatchService watcher;
    private final AtomicBoolean notShutdown = new AtomicBoolean(true);
    private final AtomicReference<Optional<JabRefException>> monitorFailure = new AtomicReference<>(Optional.empty());

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            this.watcher = watchService;
            monitorFailure.set(Optional.empty());

            while (notShutdown.get()) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException | ClosedWatchServiceException e) {
                    return;
                }

                Path directory = watchKeyToPath.get(key);
                if (directory == null) {
                    key.reset();
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        Thread.yield();
                        continue;
                    }

                    @SuppressWarnings("unchecked") WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();
                    Path fullPath = directory.resolve(filename);

                    notifyListeners(directory, fullPath, kind);

                    // If a new directory is created, register it for watching
                    if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(fullPath)) {
                        try {
                            registerDirectory(fullPath, true);
                        } catch (IOException e) {
                            LOGGER.warn("Could not register new directory for watching: {}", fullPath, e);
                        }
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    watchKeyToPath.remove(key);
                }

                Thread.yield();
            }
        } catch (IOException e) {
            JabRefException exception = new WatchServiceUnavailableException(e.getMessage(), e.getLocalizedMessage(), e.getCause());
            monitorFailure.set(Optional.of(exception));
            LOGGER.warn("Error during directory watching", e);
        }
    }

    private void notifyListeners(Path directory, Path fullPath, WatchEvent.Kind<?> kind) {
        // Find the root directory that was registered
        Path rootDir = findRootDirectory(directory);
        if (rootDir == null) {
            return;
        }

        for (DirectoryUpdateListener listener : listeners.get(rootDir)) {
            try {
                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    if (Files.isDirectory(fullPath)) {
                        listener.directoryCreated(fullPath);
                    } else {
                        listener.fileCreated(fullPath);
                    }
                } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                    if (!Files.isDirectory(fullPath)) {
                        listener.fileModified(fullPath);
                    }
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    // We can't check if it was a directory after deletion
                    // Assume it's a file unless it was tracked as a directory
                    listener.fileDeleted(fullPath);
                }
            } catch (Exception e) {
                LOGGER.warn("Error notifying listener about change in {}", fullPath, e);
            }
        }
    }

    private Path findRootDirectory(Path directory) {
        // Walk up the path to find the registered root directory
        Path current = directory;
        while (current != null) {
            if (listeners.containsKey(current)) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    @Override
    public void addListenerForDirectory(Path directory, DirectoryUpdateListener listener, boolean recursive) throws IOException {
        if (!isActive()) {
            LOGGER.warn("Not adding listener for directory {} because the monitor isn't active", directory);
            return;
        }

        if (!Files.isDirectory(directory)) {
            throw new IOException("Path is not a directory: " + directory);
        }

        listeners.put(directory, listener);
        registerDirectory(directory, recursive);
        LOGGER.debug("Added listener for directory: {}", directory);
    }

    private void registerDirectory(Path directory, boolean recursive) throws IOException {
        if (recursive) {
            // Register all subdirectories
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    registerSingleDirectory(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            registerSingleDirectory(directory);
        }
    }

    private void registerSingleDirectory(Path directory) throws IOException {
        WatchKey key = directory.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        watchKeyToPath.put(key, directory);
    }

    @Override
    public void removeListener(Path directory, DirectoryUpdateListener listener) {
        listeners.remove(directory, listener);
        LOGGER.debug("Removed listener for directory: {}", directory);
    }

    @Override
    public boolean isActive() {
        return monitorFailure.get().isEmpty() && watcher != null;
    }

    @Override
    public void shutdown() {
        try {
            notShutdown.set(false);
            WatchService watchService = this.watcher;
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException e) {
            LOGGER.error("Error closing directory watcher", e);
        }
    }
}
