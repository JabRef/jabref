package org.jabref.logic.autosaveandbackup;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

public class BackupManagerTest {

    @Test
    public void backupFileNameIsCorrectlyGeneratedWithinTmpDirectory() {
        Path bibPath = Paths.get("tmp", "test.bib");
        Path savPath = BackupManager.getBackupPath(bibPath);
        Assert.assertEquals(Paths.get("tmp", "test.bib.sav"), savPath);
    }

}
