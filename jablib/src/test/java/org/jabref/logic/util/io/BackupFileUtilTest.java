package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.Directories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BackupFileUtilTest {

    Path backupDir;

    @BeforeEach
    void setup(@TempDir Path tempDir) {
        backupDir = tempDir.resolve("backup");
    }

    @Test
    void uniqueFilePrefix() {
        // We cannot test for a concrete hash code, because hashing implementation differs from environment to environment
        assertNotEquals("", BackupFileUtil.getUniqueFilePrefix(Path.of("test.bib")));
    }

    @Test
    void getPathOfBackupFileAndCreateDirectoryReturnsAppDirectoryInCaseOfNoError() {
        String start = Directories.getBackupDirectory().toString();
        backupDir = Directories.getBackupDirectory();
        String result = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(Path.of("test.bib"), BackupFileType.BACKUP, backupDir).toString();
        // We just check the prefix
        assertEquals(start, result.substring(0, start.length()));
    }

    @Test
    void getPathOfBackupFileAndCreateDirectoryReturnsSameDirectoryInCaseOfException() {
        backupDir = Directories.getBackupDirectory();
        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class, Answers.CALLS_REAL_METHODS)) { // REAL_METHODS are required because of class loading of "BackupFileType"
            files.when(() -> Files.createDirectories(Directories.getBackupDirectory()))
                 .thenThrow(new IOException());
            Path testPath = Path.of("tmp", "test.bib");
            Path result = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(
                    testPath,
                    BackupFileType.BACKUP,
                    backupDir);
            // The intended fallback behavior is to put the .bak file in the same directory as the .bib file
            assertEquals(Path.of("tmp", "test.bib.bak"), result);
        }
    }
}
