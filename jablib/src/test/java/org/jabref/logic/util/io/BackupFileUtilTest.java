package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
            // The intended fallback behavior is to put the backup file next to the library, using the same name pattern
            assertEquals(Path.of("tmp").toAbsolutePath(), result.getParent());
            String fileNameWithoutTimestamp = result.getFileName().toString().replaceAll("\\d{4}-\\d{2}-\\d{2}--\\d{2}\\.\\d{2}\\.\\d{2}", "<timestamp>");
            assertEquals(BackupFileUtil.getUniqueFilePrefix(testPath) + "--test.bib--<timestamp>.bib", fileNameWithoutTimestamp);
        }
    }

    @Test
    void latestBackupNextToLibraryIsFoundWhenBackupDirectoryIsAbsent(@TempDir Path tempDir) throws IOException {
        Path library = tempDir.resolve("test.bib");
        String prefix = BackupFileUtil.getUniqueFilePrefix(library) + "--test.bib--";
        Files.writeString(tempDir.resolve(prefix + "2024-01-01--00.00.00.bib"), "");
        Path latestBackup = Files.writeString(tempDir.resolve(prefix + "2025-01-01--00.00.00.bib"), "");
        Files.writeString(tempDir.resolve("test.bib.bak"), "");

        assertEquals(Optional.of(latestBackup), BackupFileUtil.getPathOfLatestExistingBackupFile(library, tempDir.resolve("missing")));
    }

    @Test
    void legacyBackupNextToLibraryIsFoundWhenNoOtherBackupExists(@TempDir Path tempDir) throws IOException {
        Path library = tempDir.resolve("test.bib");
        Path legacyBackup = Files.writeString(tempDir.resolve("test.bib.bak"), "");

        assertEquals(Optional.of(legacyBackup), BackupFileUtil.getPathOfLatestExistingBackupFile(library, tempDir.resolve("missing")));
    }
}
