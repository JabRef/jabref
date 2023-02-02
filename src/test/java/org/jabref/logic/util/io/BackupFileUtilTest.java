package org.jabref.logic.util.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.util.BackupFileType;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class BackupFileUtilTest {

    @Test
    void uniqueFilePrefix() {
        // We cannot test for a concrete hash code, because hashing implementation differs from environment to environment
        assertNotEquals("", BackupFileUtil.getUniqueFilePrefix(Path.of("test.bib")));
    }

    @Test
    void getPathOfBackupFileAndCreateDirectoryReturnsAppDirectoryInCaseOfNoError() {
        String start = BackupFileUtil.getAppDataBackupDir().toString();
        String result = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(Path.of("test.bib"), BackupFileType.BACKUP).toString();
        // We just check the prefix
        assertEquals(start, result.substring(0, start.length()));
    }

    @Test
    void getPathOfBackupFileAndCreateDirectoryReturnsSameDirectoryInCaseOfException() {
        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class, Answers.RETURNS_DEEP_STUBS)) {
            files.when(() -> Files.createDirectories(BackupFileUtil.getAppDataBackupDir()))
                 .thenThrow(new IOException());
            Path testPath = Path.of("tmp", "test.bib");
            Path result = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(testPath, BackupFileType.BACKUP);
            // The intended fallback behavior is to put the .bak file in the same directory as the .bib file
            assertEquals(Path.of("tmp", "test.bib.bak"), result);
        }
    }
}
