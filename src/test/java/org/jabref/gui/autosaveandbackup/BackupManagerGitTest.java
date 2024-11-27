package org.jabref.gui.autosaveandbackup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;

import org.jabref.gui.LibraryTab;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackupManagerGitTest {

    Path backupDir;

    @BeforeEach
    void setup(@TempDir Path tempDir) throws IOException, GitAPIException {
        backupDir = tempDir.resolve("backup");
    }

    @Test
    void testGitRepositoryExistsAfterInitialization() throws IOException, GitAPIException {
        // Initialize BackupManagerGit
        BackupManagerGit backupManager = new BackupManagerGit(libraryTab, bibDatabaseContext, entryTypesManager, preferences);

        // Check if the .git directory exists
        Path gitDir = backupDir.resolve(".git");
        assertTrue(Files.exists(gitDir), "Git repository should exist after initialization");
    }

    @Test
    void testBackupFileIsEqualForNonExistingBackup() throws Exception {
        Path originalFile = Path.of(BackupManagerGitTest.class.getResource("no-autosave.bib").toURI());
        assertFalse(BackupManagerGit.backupGitDiffers(originalFile, backupDir));
    }

    @Test
    void testBackupFileIsEqual() throws Exception {
        // Prepare test: Create backup file on "right" path
        Path source = Path.of(BackupManagerGitTest.class.getResource("no-changes.bib.bak").toURI());
        Path target = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(Path.of(BackupManagerGitTest.class.getResource("no-changes.bib").toURI()), BackupFileType.BACKUP, backupDir);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        Path originalFile = Path.of(BackupManagerGitTest.class.getResource("no-changes.bib").toURI());
        assertFalse(BackupManagerGit.backupGitDiffers(originalFile, backupDir));
    }

    @Test
    void testBackupFileDiffers() throws Exception {
        // Prepare test: Create backup file on "right" path
        Path source = Path.of(BackupManagerGitTest.class.getResource("changes.bib.bak").toURI());
        Path target = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(Path.of(BackupManagerGitTest.class.getResource("changes.bib").toURI()), BackupFileType.BACKUP, backupDir);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        Path originalFile = Path.of(BackupManagerGitTest.class.getResource("changes.bib").toURI());
        assertTrue(BackupManagerGit.backupGitDiffers(originalFile, backupDir));
    }

    @Test
    void testCorrectBackupFileDeterminedForMultipleBakFiles() throws Exception {
        Path noChangesBib = Path.of(BackupManagerGitTest.class.getResource("no-changes.bib").toURI());
        Path noChangesBibBak = Path.of(BackupManagerGitTest.class.getResource("no-changes.bib.bak").toURI());

        // Prepare test: Create backup files on "right" path
        // most recent file does not have any changes
        Path target = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(noChangesBib, BackupFileType.BACKUP, backupDir);
        Files.copy(noChangesBibBak, target, StandardCopyOption.REPLACE_EXISTING);

        // create "older" .bak files containing changes
        for (int i = 0; i < 10; i++) {
            Path changesBibBak = Path.of(BackupManagerGitTest.class.getResource("changes.bib").toURI());
            Path directory = backupDir;
            String timeSuffix = "2020-02-03--00.00.0" + Integer.toString(i);
            String fileName = BackupFileUtil.getUniqueFilePrefix(noChangesBib) + "--no-changes.bib--" + timeSuffix + ".bak";
            target = directory.resolve(fileName);
            Files.copy(changesBibBak, target, StandardCopyOption.REPLACE_EXISTING);
        }

        Path originalFile = noChangesBib;
        assertFalse(BackupManagerGit.backupGitDiffers(originalFile, backupDir));
    }

    @Test
    void testBakFileWithNewerTimeStampLeadsToDiff() throws Exception {
        Path changesBib = Path.of(BackupManagerGitTest.class.getResource("changes.bib").toURI());
        Path changesBibBak = Path.of(BackupManagerGitTest.class.getResource("changes.bib.bak").toURI());

        Path target = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(changesBib, BackupFileType.BACKUP, backupDir);
        Files.copy(changesBibBak, target, StandardCopyOption.REPLACE_EXISTING);

        assertTrue(BackupManagerGit.backupGitDiffers(changesBib, backupDir));
    }

    @Test
    void testBakFileWithOlderTimeStampDoesNotLeadToDiff() throws Exception {
        Path changesBib = Path.of(BackupManagerGitTest.class.getResource("changes.bib").toURI());
        Path changesBibBak = Path.of(BackupManagerGitTest.class.getResource("changes.bib.bak").toURI());

        Path target = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(changesBib, BackupFileType.BACKUP, backupDir);
        Files.copy(changesBibBak, target, StandardCopyOption.REPLACE_EXISTING);

        // Make .bak file very old
        Files.setLastModifiedTime(target, FileTime.fromMillis(0));

        assertFalse(BackupManagerGit.backupGitDiffers(changesBib, backupDir));
    }
}




