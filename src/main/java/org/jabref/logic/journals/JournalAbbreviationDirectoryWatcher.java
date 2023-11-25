package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalAbbreviationDirectoryWatcher implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationDirectoryWatcher.class);
    private final List<Runnable> journalAbbreviationDirectoryOnChangeActions = new ArrayList<>();
    private final Map<String, Consumer<Path>> journalExternalFilesActions = new HashMap<>();
    private WatchService watchService;
    private JournalAbbreviationPreferences journalAbbreviationPreferences;

    public JournalAbbreviationDirectoryWatcher(JournalAbbreviationPreferences journalAbbreviationPreferences) {
        try {
            this.journalAbbreviationPreferences = journalAbbreviationPreferences;
            initWatchService();
            initJournalExternalFilesActionsMap();
        } catch (
                IOException e) {
            LOGGER.error("Could not create watch service", e);
        }
    }

    private void initJournalExternalFilesActionsMap() {
        journalExternalFilesActions.put(StandardWatchEventKinds.ENTRY_CREATE.name(), (path) -> {
            journalAbbreviationPreferences.getExternalJournalLists().add(String.valueOf(
                    journalAbbreviationPreferences.getJournalAbbreviationsDirectory().resolve(path)));
        });
        journalExternalFilesActions.put(StandardWatchEventKinds.ENTRY_DELETE.name(), (path) -> {
            journalAbbreviationPreferences.getExternalJournalLists().remove(String.valueOf(
                    journalAbbreviationPreferences.getJournalAbbreviationsDirectory().resolve(path)));
        });
    }

    private void initWatchService() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();
        journalAbbreviationPreferences.getJournalAbbreviationsDirectory().register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
    }

    public void registerOnChangeAction(Runnable runnable) {
        journalAbbreviationDirectoryOnChangeActions.add(runnable);
    }

    @Override
    public void run() {
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
                Consumer<Path> journalExternalFilesAction = journalExternalFilesActions.get(event.kind().name());
                if (Objects.nonNull(journalExternalFilesAction)) {
                    journalExternalFilesAction.accept((Path) event.context());
                }
            });
            journalAbbreviationDirectoryOnChangeActions.forEach(Runnable::run);
        }
    }
}
