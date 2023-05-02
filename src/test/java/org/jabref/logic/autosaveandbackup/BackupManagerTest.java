package org.jabref.logic.autosaveandbackup;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;

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
        Path noChangesBib = Path.of(BackupManagerTest.class.getResource("no-changes.bib").toURI());
        Path noChangesBibBak = Path.of(BackupManagerTest.class.getResource("no-changes.bib.bak").toURI());

        // Prepare test: Create backup files on "right" path
        // most recent file does not have any changes
        Path target = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(noChangesBib, BackupFileType.BACKUP);
        Files.copy(noChangesBibBak, target, StandardCopyOption.REPLACE_EXISTING);

        // create "older" .bak files containing changes
        for (int i = 0; i < 10; i++) {
            Path changesBibBak = Path.of(BackupManagerTest.class.getResource("changes.bib").toURI());
            Path directory = BackupFileUtil.getAppDataBackupDir();
            String timeSuffix = "2020-02-03--00.00.0" + Integer.toString(i);
            String fileName = BackupFileUtil.getUniqueFilePrefix(noChangesBib) + "--no-changes.bib--" + timeSuffix + ".bak";
            target = directory.resolve(fileName);
            Files.copy(changesBibBak, target, StandardCopyOption.REPLACE_EXISTING);
        }

        Path originalFile = noChangesBib;
        assertFalse(BackupManager.backupFileDiffers(originalFile));
    }

    @Test
    public void bakFileWithNewerTimeStampLeadsToDiff() throws Exception {
        Path changesBib = Path.of(BackupManagerTest.class.getResource("changes.bib").toURI());
        Path changesBibBak = Path.of(BackupManagerTest.class.getResource("changes.bib.bak").toURI());

        Path target = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(changesBib, BackupFileType.BACKUP);
        Files.copy(changesBibBak, target, StandardCopyOption.REPLACE_EXISTING);

        assertTrue(BackupManager.backupFileDiffers(changesBib));
    }

    @Test
    public void bakFileWithOlderTimeStampDoesNotLeadToDiff() throws Exception {
        Path changesBib = Path.of(BackupManagerTest.class.getResource("changes.bib").toURI());
        Path changesBibBak = Path.of(BackupManagerTest.class.getResource("changes.bib.bak").toURI());

        Path target = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(changesBib, BackupFileType.BACKUP);
        Files.copy(changesBibBak, target, StandardCopyOption.REPLACE_EXISTING);

        // Make .bak file very old
        Files.setLastModifiedTime(target, FileTime.fromMillis(0));

        assertFalse(BackupManager.backupFileDiffers(changesBib));
    }
}
