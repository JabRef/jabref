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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jabref.logic.JabRefException;
import org.jabref.logic.WatchServiceUnavailableException;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.groups.DirectoryGroup;
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
                    System.out.println(kind);
                    System.out.println(listeners.keys());
                    cleanupListenersAccordingToJabRefChanges();
                    System.out.println(listeners.keys());

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        Thread.yield();
                        continue;
                    } else if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        // We only handle "ENTRY_CREATE" here, so the context is always a Path
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path path = ((Path) key.watchable()).resolve(ev.context());
                        if (Files.exists(path)) {
                            if (Files.isDirectory(path) && !listeners.containsKey(path)) {
                                System.out.println("Create a directory : " + path + " : " + ev.context());
                                notifyAboutDirectoryCreation(path);
                            } else {
                                if (FileUtil.isPDFFile(path)) {
                                    // notifyAboutPDFCreation(path);
                                }
                            }
                        }
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        // We only handle "ENTRY_MODIFY" here, so the context is always a Path
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path path = ((Path) key.watchable()).resolve(ev.context());
                        if (Files.exists(path)) {
                            if (Files.isDirectory(path)) {
                                System.out.println("Probably a useless call : " + path + " : " + ev.context());
                            } else {
                                notifyAboutFileChange(path);
                            }
                        }
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        // We only handle "ENTRY_DELETE" here, so the context is always a Path
                        @SuppressWarnings("unchecked")
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path path = ((Path) key.watchable()).resolve(ev.context());
                        System.out.println("Delete a directory : " + path + " : " + ev.context());
                        notifyAboutDirectoryDeletion(path);
                        cleanupListenersAccordingToLocalChanges();
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

    private void notifyAboutDirectoryCreation(Path newPath) {
        Path parentPath = newPath.toAbsolutePath().getParent();
        for (DirectoryUpdateListener listener : listeners.get(parentPath)) {
            if (listener instanceof DirectoryGroup parentGroup) {
                try {
                    parentGroup.directoryCreated(newPath);
                } catch (IOException e) {
                    LOGGER.error("Error while creating directory {}", newPath, e);
                }
            }
        }
        System.out.println("New directory: " + newPath);
    }

    private void notifyAboutDirectoryDeletion(Path deletedPath) {
        for (DirectoryUpdateListener listener : listeners.get(deletedPath)) {
            if (listener instanceof DirectoryGroup deletedGroup) {
                deletedGroup.directoryDeleted();
            }
        }
    }

    private void notifyAboutPDFCreation(Path PDFPath) {
        Path parentPath = PDFPath.toAbsolutePath().getParent();
        for (DirectoryUpdateListener listener : listeners.get(parentPath)) {
            if (listener instanceof DirectoryGroup pdfGroup) {
                // TODO : import the PDF
            }
        }
    }

    private void notifyAboutFileChange(Path path) {
        listeners.get(path).forEach(DirectoryUpdateListener::fileUpdated);
    }

    @Override
    public void addListenerForDirectory(Path directory, DirectoryUpdateListener listener) throws IOException {
        if (isActive()) {
            directory.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
            listeners.put(directory, listener);
        } else {
            LOGGER.warn("Not adding listener {} to directory {} because the directory update monitor isn't active", listener, directory);
        }
    }

    @Override
    public void removeListener(Path path, DirectoryUpdateListener listener) {
        listeners.remove(path, listener);
    }

    public void cleanupListenersAccordingToJabRefChanges() {
        Multimap<Path, DirectoryUpdateListener> watchersToRemove = ArrayListMultimap.create();
        for (Map.Entry<Path, DirectoryUpdateListener> entry : listeners.entries()) {
            Path registeredPath = entry.getKey();
            DirectoryUpdateListener registeredListener = entry.getValue();
            if (registeredListener instanceof DirectoryGroup directoryGroup && directoryGroup.isDeleted()) {
                watchersToRemove.put(registeredPath, registeredListener);
            }
        }
        for (Map.Entry<Path, DirectoryUpdateListener> entry : watchersToRemove.entries()) {
            listeners.remove(entry.getKey(), entry.getValue());
        }
    }

    public void cleanupListenersAccordingToLocalChanges() {
        List<Path> deletedPaths = new ArrayList<>();
        for (Path registeredPath : listeners.keys()) {
            if (registeredPath != null && !Files.exists(registeredPath)) {
                deletedPaths.add(registeredPath);
            }
        }
        for (Path deletedPath : deletedPaths) {
            listeners.removeAll(deletedPath);
        }
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
