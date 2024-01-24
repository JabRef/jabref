package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalAbbreviationDirectoryWatcher implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationDirectoryWatcher.class);
    private final List<JournalAbbreviationDirectoryChangeListener> listeners = new ArrayList<>();
    private WatchService watchService;
    private JournalAbbreviationPreferences journalAbbreviationPreferences;

    public JournalAbbreviationDirectoryWatcher(JournalAbbreviationPreferences journalAbbreviationPreferences) {
        try {
            this.journalAbbreviationPreferences = journalAbbreviationPreferences;
            initWatchService();
        } catch (
                IOException e) {
            LOGGER.error("Could not create watch service", e);
        }
    }

    private void initWatchService() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        journalAbbreviationPreferences.getJournalAbbreviationsDirectory().getValue().register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
    }

    public void registerListener(JournalAbbreviationDirectoryChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void run() {
        LOGGER.info("Directory watcher started");
        while (true) {
            if (Thread.interrupted()) {
                LOGGER.info("Directory watcher interrupted.");
                break;
            }

            WatchKey watchKey;
            try {
                watchKey = watchService.take();
            } catch (
                    InterruptedException e) {
                Thread.currentThread().interrupt();
                continue;
            }
            watchKey.pollEvents().forEach(event -> {
                if (isValidEvent(event)) {
                    listeners.forEach(listener -> listener.onJournalAbbreviationDirectoryChangeListener((Path) watchKey.watchable(), (WatchEvent<Path>) event));
                }
            });
            watchKey.reset();
        }
    }

    private boolean isValidEvent(WatchEvent<?> event) {
        return event.context() instanceof Path && JournalAbbreviationsUtils.isCSVFile((Path) event.context());
    }

    public List<JournalAbbreviationDirectoryChangeListener> getListeners() {
        return listeners;
    }
}
