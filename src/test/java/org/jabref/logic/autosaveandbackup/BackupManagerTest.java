package org.jabref.logic.autosaveandbackup;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BackupManagerTest {

    @Test
    public void autosaveFileNameIsCorrectlyGeneratedWithinTmpDirectory() {
        Path bibPath = Path.of("tmp", "test.bib");
        Path savPath = BackupManager.getBackupPath(bibPath);
        assertEquals(Path.of("tmp", "test.bib.sav"), savPath);
    }

    @Test
    public void autosaveFileIsEqualForNonExistingBackup() throws Exception {
        Path originalFile = Path.of(BackupManagerTest.class.getResource("no-autosave.bib").toURI());
        assertFalse(BackupManager.backupFileDiffers(originalFile));
    }

    @Test
    public void backupFileIsEqual() throws Exception {
        Path originalFile = Path.of(BackupManagerTest.class.getResource("no-changes.bib").toURI());
        assertFalse(BackupManager.backupFileDiffers(originalFile));
    }

    @Test
    public void backupFileDiffers() throws Exception {
        Path originalFile = Path.of(BackupManagerTest.class.getResource("changes.bib").toURI());
        assertTrue(BackupManager.backupFileDiffers(originalFile));
    }
}
