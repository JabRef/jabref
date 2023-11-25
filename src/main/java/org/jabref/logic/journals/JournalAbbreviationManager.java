package org.jabref.logic.journals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalAbbreviationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationManager.class);
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();
    private static JournalAbbreviationDirectoryWatcher journalAbbreviationDirectoryWatcher;
    public static void init(JournalAbbreviationPreferences journalAbbreviationPreferences) {
        File journalAbbreviationsDirectory = createJournalAbbreviationsDirectory(journalAbbreviationPreferences.getJournalAbbreviationsDirectory());
        createDefaultJournalAbbreviationsFile(journalAbbreviationsDirectory);
        journalAbbreviationDirectoryWatcher = new JournalAbbreviationDirectoryWatcher(journalAbbreviationPreferences);
        EXECUTOR_SERVICE.submit(journalAbbreviationDirectoryWatcher);
    }

    private static File createJournalAbbreviationsDirectory(Path journalAbbreviationsDirectory) {
        try {
            return Files.createDirectory(journalAbbreviationsDirectory).toFile();
        } catch (
                IOException e) {
            LOGGER.error("Could not create default journal abbreviation directory {}", journalAbbreviationsDirectory, e);
        }
        return null;
    }

    private static void createDefaultJournalAbbreviationsFile(File journalAbbreviationsDirectory) {
        File defaultJournalAbbreviations = new File(journalAbbreviationsDirectory, "custom.csv");
        try {
            defaultJournalAbbreviations.createNewFile();
        } catch (
                IOException e) {
            LOGGER.error("Could not create default journal abbreviation file {}", defaultJournalAbbreviations, e);
        }
    }

    public static void registerRefreshAction(Runnable refreshAction) {
        journalAbbreviationDirectoryWatcher.registerOnChangeAction(refreshAction);
    }
}
