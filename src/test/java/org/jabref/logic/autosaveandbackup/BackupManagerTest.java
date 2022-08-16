package org.jabref.logic.autosaveandbackup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.io.BackupFileUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BackupManagerTest {

    @Test
    public void backupFileNameIsCorrectlyGeneratedInAppDataDirectory() {
        Path bibPath = Path.of("tmp", "test.bib");
        Path bakPath = BackupManager.getBackupPathForNewBackup(bibPath);

        // Pattern is "27182d3c--test.bib--", but the hashing is implemented differently on Linux than on Windows
        assertNotEquals("", bakPath);
    }

    @Test
    public void backupFileIsEqualForNonExistingBackup() throws Exception {
        Path originalFile = Path.of(BackupManagerTest.class.getResource("no-autosave.bib").toURI());
        assertFalse(BackupManager.backupFileDiffers(originalFile));
    }

    @Test
    public void backupFileIsEqual() throws Exception {
        // Prepare test: Create backup file on "right" path
        Path source = Path.of(BackupManagerTest.class.getResource("no-changes.bib.bak").toURI());
        Path target = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(Path.of(BackupManagerTest.class.getResource("no-changes.bib").toURI()), BackupFileType.BACKUP);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        Path originalFile = Path.of(BackupManagerTest.class.getResource("no-changes.bib").toURI());
        assertFalse(BackupManager.backupFileDiffers(originalFile));
    }

    @Test
    public void backupFileDiffers() throws Exception {
        // Prepare test: Create backup file on "right" path
        Path source = Path.of(BackupManagerTest.class.getResource("changes.bib.bak").toURI());
        Path target = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(Path.of(BackupManagerTest.class.getResource("changes.bib").toURI()), BackupFileType.BACKUP);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        Path originalFile = Path.of(BackupManagerTest.class.getResource("changes.bib").toURI());
        assertTrue(BackupManager.backupFileDiffers(originalFile));
    }

    @Test
    public void correctBackupFileDeterminedForMultipleBakFiles() throws Exception {
        // Prepare test: Create backup files on "right" path

        // most recent file does not have any changes
        Path source = Path.of(BackupManagerTest.class.getResource("no-changes.bib.bak").toURI());
        Path target = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(Path.of(BackupManagerTest.class.getResource("no-changes.bib").toURI()), BackupFileType.BACKUP);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        // create "older" .bak files containing changes
        for (int i = 0; i < 10; i++) {
            source = Path.of(BackupManagerTest.class.getResource("changes.bib").toURI());
            Path directory = BackupFileUtil.getAppDataBackupDir();
            String timeSuffix = "2020-02-03--00.00.0" + Integer.toString(i);
            String fileName = BackupFileUtil.getUniqueFilePrefix(Path.of(BackupManagerTest.class.getResource("no-changes.bib").toURI())) + "--no-changes.bib--" + timeSuffix + ".bak";
            target = directory.resolve(fileName);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }

        Path originalFile = Path.of(BackupManagerTest.class.getResource("no-changes.bib").toURI());
        assertFalse(BackupManager.backupFileDiffers(originalFile));
    }
}
