package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Optional;

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
            // The intended fallback behavior is to put the backup file in the same directory as the .bib file
            assertEquals(Path.of("tmp", "test.bib.bib"), result);
        }
    }

    @Test
    void legacyBakSidecarIsFoundWhenBackupDirectoryIsAbsent(@TempDir Path tempDir) throws IOException {
        Path library = tempDir.resolve("test.bib");
        Path legacyBackup = Files.writeString(tempDir.resolve("test.bib.bak"), "");

        assertEquals(Optional.of(legacyBackup), BackupFileUtil.getPathOfLatestExistingBackupFile(library, BackupFileType.BACKUP, tempDir.resolve("missing")));
    }

    @Test
    void newestSidecarWinsWhenBackupDirectoryIsAbsent(@TempDir Path tempDir) throws IOException {
        Path library = tempDir.resolve("test.bib");
        Path absentBackupDir = tempDir.resolve("missing");
        Path legacyBackup = Files.writeString(tempDir.resolve("test.bib.bak"), "");
        Path currentBackup = Files.writeString(tempDir.resolve("test.bib.bib"), "");

        Files.setLastModifiedTime(legacyBackup, FileTime.fromMillis(2_000));
        Files.setLastModifiedTime(currentBackup, FileTime.fromMillis(1_000));
        assertEquals(Optional.of(legacyBackup), BackupFileUtil.getPathOfLatestExistingBackupFile(library, BackupFileType.BACKUP, absentBackupDir));

        Files.setLastModifiedTime(currentBackup, FileTime.fromMillis(3_000));
        assertEquals(Optional.of(currentBackup), BackupFileUtil.getPathOfLatestExistingBackupFile(library, BackupFileType.BACKUP, absentBackupDir));
    }
}
