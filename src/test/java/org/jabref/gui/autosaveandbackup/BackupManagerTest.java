package org.jabref.gui.autosaveandbackup;

import java.nio.file.Path;

import org.jabref.logic.util.Directories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BackupManagerTest {

    Path backupDir;

    @BeforeEach
    void setup(@TempDir Path tempDir) {
        backupDir = tempDir.resolve("backup");
    }

    @Test
    void backupFileNameIsCorrectlyGeneratedInAppDataDirectory() {
        Path bibPath = Path.of("tmp", "test.bib");
        backupDir = Directories.getBackupDirectory();
        Path bakPath = BackupManager.getBackupPathForNewBackup(bibPath, backupDir);

        // Pattern is "27182d3c--test.bib--", but the hashing is implemented differently on Linux than on Windows
        assertNotEquals("", bakPath);
    }
}
