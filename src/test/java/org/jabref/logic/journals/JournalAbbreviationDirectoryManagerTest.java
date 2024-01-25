package org.jabref.logic.journals;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;

import javafx.collections.FXCollections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JournalAbbreviationDirectoryManagerTest {

    JournalAbbreviationPreferences preferences;
    File workDir;

    @BeforeEach
    public void setup() {
        workDir = new File("journalAbbrDirTest");
        assertTrue(workDir.mkdirs());
        preferences = new JournalAbbreviationPreferences(Collections.emptyList(), false, Path.of(workDir.getAbsolutePath()));
        JournalAbbreviationDirectoryManager.init(preferences);
    }

    @Test
    public void shouldInitCorrectly() {
        File customCsvFile = new File(workDir.getAbsolutePath(), "custom.csv");
        assertTrue(customCsvFile.exists());

        assertIterableEquals(FXCollections.observableArrayList(customCsvFile.getAbsolutePath()), preferences.getExternalJournalLists());
    }

    @Test
    public void shouldCorrectlyBehaveOnDirectoryChange() {
        File newWorkDir = new File("newJournalAbbrDirTest");
        JournalAbbreviationPreferences newPreferences = new JournalAbbreviationPreferences(Collections.emptyList(), false, Path.of(newWorkDir.getAbsolutePath()));
        JournalAbbreviationDirectoryManager.onDirectoryChange(newPreferences);
        File customCsvFile = new File(newWorkDir.getAbsolutePath(), "custom.csv");
        assertTrue(customCsvFile.exists());

        assertIterableEquals(FXCollections.observableArrayList(customCsvFile.getAbsolutePath()), newPreferences.getExternalJournalLists());
        cleanupDir(newWorkDir);
    }

    @AfterEach
    public void cleanup() {
        JournalAbbreviationDirectoryManager.tearDown();
        cleanupDir(workDir);
    }

    public void cleanupDir(File dir) {
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            assertTrue(file.delete());
        }
        assertTrue(dir.delete());
    }
}

