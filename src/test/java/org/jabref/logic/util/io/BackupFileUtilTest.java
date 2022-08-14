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

public class BackupFileUtilTest {

    @Test
    void uniqueFilePrefix() {
        // The number "7001d6e0" is "random"
        assertEquals("7001d6e0", BackupFileUtil.getUniqueFilePrefix(Path.of("/tmp/test.bib")));
    }

    @Test
    void getPathOfBackupFileAndCreateDirectoryReturnsAppDirectoryInCaseOfNoError() {
        String start = BackupFileUtil.getAppDataBackupDir().toString();
        String result = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(Path.of("/tmp/test.bib"), BackupFileType.BACKUP).toString();
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
            // We just check the prefix
            assertEquals(Path.of("tmp", "test.bib.bak"), result);
        }
    }
}
