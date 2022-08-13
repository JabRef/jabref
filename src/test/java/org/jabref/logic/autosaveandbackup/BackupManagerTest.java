package org.jabref.logic.autosaveandbackup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.io.FileUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BackupManagerTest {

    @Test
    public void autosaveFileNameIsCorrectlyGeneratedInAppDataDirectory() {
        Path bibPath = Path.of("tmp", "test.bib");
        Path savPath = BackupManager.getBackupPathForNewBackup(bibPath);

        assertEquals(FileUtil.getAppDataBackupDir(), savPath.getParent());
        String start = savPath.getFileName().toString().substring(0, 20); // Timestamp will differ
        assertEquals("27182d3c--test.bib--", start);
    }

    @Test
    public void autosaveFileIsEqualForNonExistingBackup() throws Exception {
        Path originalFile = Path.of(BackupManagerTest.class.getResource("no-autosave.bib").toURI());
        assertFalse(BackupManager.backupFileDiffers(originalFile));
    }

    @Test
    public void backupFileIsEqual() throws Exception {
        // Prepare test: Create backup file on "right" path
        Path source = Path.of(BackupManagerTest.class.getResource("no-changes.bib.sav").toURI());
        Path target = FileUtil.getPathForNewBackupFileAndCreateDirectory(Path.of(BackupManagerTest.class.getResource("no-changes.bib").toURI()), BackupFileType.AUTOSAVE);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        Path originalFile = Path.of(BackupManagerTest.class.getResource("no-changes.bib").toURI());
        assertFalse(BackupManager.backupFileDiffers(originalFile));
    }

    @Test
    public void backupFileDiffers() throws Exception {
        // Prepare test: Create backup file on "right" path
        Path source = Path.of(BackupManagerTest.class.getResource("changes.bib.sav").toURI());
        Path target = FileUtil.getPathForNewBackupFileAndCreateDirectory(Path.of(BackupManagerTest.class.getResource("changes.bib").toURI()), BackupFileType.AUTOSAVE);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        Path originalFile = Path.of(BackupManagerTest.class.getResource("changes.bib").toURI());
        assertTrue(BackupManager.backupFileDiffers(originalFile));
    }

    @Test
    public void correctBackupFileDeterminedForMultipleSavFiles() throws Exception {
        // Prepare test: Create backup files on "right" path

        // most recent file does not have any changes
        Path source = Path.of(BackupManagerTest.class.getResource("no-changes.bib.sav").toURI());
        Path target = FileUtil.getPathForNewBackupFileAndCreateDirectory(Path.of(BackupManagerTest.class.getResource("no-changes.bib").toURI()), BackupFileType.AUTOSAVE);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        // create "older" .sav files containing changes
        for (int i = 0; i < 10; i++) {
            source = Path.of(BackupManagerTest.class.getResource("changes.bib").toURI());
            Path directory = FileUtil.getAppDataBackupDir();
            String timeSuffix = "2020-02-03--00.00.0" + Integer.toString(i);
            String fileName = FileUtil.getUniqueFilePrefix(Path.of(BackupManagerTest.class.getResource("no-changes.bib").toURI())) + "--no-changes.bib--" + timeSuffix + ".sav";
            target = directory.resolve(fileName);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }

        Path originalFile = Path.of(BackupManagerTest.class.getResource("no-changes.bib").toURI());
        assertFalse(BackupManager.backupFileDiffers(originalFile));
    }
}
