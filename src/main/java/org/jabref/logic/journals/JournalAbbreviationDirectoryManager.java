package org.jabref.logic.journals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalAbbreviationDirectoryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationDirectoryManager.class);
    private static JournalAbbreviationDirectoryWatcher directoryWatcher;
    private static Thread directoryWatcherThread;
    public static void init(JournalAbbreviationPreferences preferences) {
        initJournalAbbreviationDirectory(preferences);
        startDirectoryWatcher(preferences);
        registerJournalAbbreviationDirectoryChangeListener(new JournalAbbreviationDirectorySynchronizer(preferences));
        registerJournalAbbreviationDirectoryChangeListener(new JournalAbbreviationStorer());
    }

    public static void onDirectoryChange(JournalAbbreviationPreferences preferences) {
        List<JournalAbbreviationDirectoryChangeListener> previousListeners = directoryWatcher.getListeners();
        initJournalAbbreviationDirectory(preferences);
        directoryWatcherThread.interrupt();
        startDirectoryWatcher(preferences);
        previousListeners.forEach(JournalAbbreviationDirectoryManager::registerJournalAbbreviationDirectoryChangeListener);
    }

    private static void startDirectoryWatcher(JournalAbbreviationPreferences preferences) {
        directoryWatcher = new JournalAbbreviationDirectoryWatcher(preferences);
        directoryWatcherThread = new Thread(directoryWatcher);
        directoryWatcherThread.setDaemon(true);
        directoryWatcherThread.start();
    }

    private static void initJournalAbbreviationDirectory(JournalAbbreviationPreferences preferences) {
        createJournalAbbreviationsDirectory(preferences.getJournalAbbreviationsDirectory().getValue());
        createDefaultJournalAbbreviationsFile(new File(preferences.getJournalAbbreviationsDirectory().getValue().toString()));
    }

    private static void createJournalAbbreviationsDirectory(Path journalAbbreviationsDirectory) {
        try {
            Files.createDirectory(journalAbbreviationsDirectory).toFile();
        } catch (
                IOException e) {
            LOGGER.info("Journal abbreviation directory {} already exists", journalAbbreviationsDirectory);
        }
    }

    private static void createDefaultJournalAbbreviationsFile(File journalAbbreviationsDirectory) {
        File defaultJournalAbbreviations = new File(journalAbbreviationsDirectory, "custom.csv");
        try {
            defaultJournalAbbreviations.createNewFile();
        } catch (
                IOException e) {
            LOGGER.info("Default Journal Abbreviations File exists - skipping");
        }
    }

    public static void registerJournalAbbreviationDirectoryChangeListener(JournalAbbreviationDirectoryChangeListener listener) {
        directoryWatcher.registerListener(listener);
    }

    public static void tearDown() {
        directoryWatcherThread.interrupt();
    }
}
