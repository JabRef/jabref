package org.jabref.gui.autosaveandbackup;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.jabref.gui.LibraryTab;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BackupManagerGitTest {

    @TempDir
    Path tempDir;
    Path backupDir;
    Git git;
    CliPreferences preferences;
    BibEntryTypesManager entryTypesManager;
    LibraryTab libraryTab;
    BibDatabaseContext bibDatabaseContext;

    @BeforeEach
    void setup() throws Exception {
        backupDir = tempDir.resolve("backup");
        Files.createDirectories(backupDir);

        // Initialize a mock Git repository in the parent directory of the backup directory
        git = Git.init().setDirectory(backupDir.getParent().toFile()).call();

        // Mock dependencies
        preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        entryTypesManager = mock(BibEntryTypesManager.class, Answers.RETURNS_DEEP_STUBS);
        libraryTab = mock(LibraryTab.class);

        when(preferences.getFilePreferences().getBackupDirectory()).thenReturn(backupDir);
    }

    @Test
    void testBackupManagerInitializesGitRepository() throws Exception {
        // Ensure the backup directory exists
        Path backupDir = tempDir.resolve("backup");
        Files.createDirectories(backupDir);

        // Create a BibDatabaseContext
        Path databaseFile = tempDir.resolve("test.bib");
        Files.writeString(databaseFile, "Initial content");
        var bibDatabaseContext = new BibDatabaseContext(new BibDatabase());
        bibDatabaseContext.setDatabasePath(databaseFile);

        // Initialize BackupManagerGit
        BackupManagerGit manager = new BackupManagerGit(libraryTab, bibDatabaseContext, entryTypesManager, preferences);

        // Ensure the Git repository is initialized in the backup directory
        assertTrue(Files.exists(backupDir.resolve(".git")), "Git repository not initialized in backup directory");
    }

    @Test
    void testBackupGitDiffers_NoDifferences() throws Exception {
        // Create a file in the original directory
        Path originalFile = tempDir.resolve("test.bib");
        Files.writeString(originalFile, "Initial content");

        // Create the backup directory if it doesn't exist
        Files.createDirectories(backupDir);

        // Copy the original file to the backup directory
        Path fileInBackupDir = backupDir.resolve("test.bib");
        Files.copy(originalFile, fileInBackupDir, StandardCopyOption.REPLACE_EXISTING);

        // Initialize the Git repository if not already done
        if (!Files.exists(backupDir.resolve(".git"))) {
            Git.init().setDirectory(backupDir.toFile()).call();
        }

        // Add and commit the file to the Git repository
        Git git = Git.open(backupDir.toFile());
        git.add().addFilepattern("test.bib").call();
        git.commit().setMessage("Initial commit").call();

        // Check that no differences are detected between the backup file and Git repository
        boolean differs = BackupManagerGit.backupGitDiffers(fileInBackupDir, backupDir);
        assertFalse(differs, "Differences were detected when there should be none.");

        // Clean up resources
        BackupManagerGit.shutdown(bibDatabaseContext, backupDir, false, originalFile);
    }

    @Test
    void testBackupGitDiffers_WithDifferences() throws Exception {
        // Create a file in the backup directory
        Path originalFile = tempDir.resolve("test.bib");
        Files.writeString(originalFile, "Initial content");

        // Copy the file to the backup directory
        Path fileInBackupDir = backupDir.resolve("test.bib");
        Files.copy(originalFile, fileInBackupDir, StandardCopyOption.REPLACE_EXISTING);

        // Add and commit the file in the Git repository
        git.add().addFilepattern(".").call();
        git.commit().setMessage("Initial commit").call();

        // Modify the file in the backup directory
        Files.writeString(fileInBackupDir, "Modified content");

        // Check that differences are detected
        boolean differs = BackupManagerGit.backupGitDiffers(fileInBackupDir, backupDir);
        assertTrue(differs);

        BackupManagerGit.shutdown(bibDatabaseContext, backupDir, differs, originalFile);
    }

    @Test
    void testNoNewRepositoryCreated() throws Exception {
        // Create a fake file to simulate the database file
        Path databaseFile = tempDir.resolve("test.bib");
        Files.writeString(databaseFile, "Initial content");

        // Set up BibDatabaseContext with the file path
        var bibDatabaseContext = new BibDatabaseContext(new BibDatabase());
        bibDatabaseContext.setDatabasePath(databaseFile);

        // Ensure the initial repository is created
        BackupManagerGit initialManager = new BackupManagerGit(libraryTab, bibDatabaseContext, entryTypesManager, preferences);
        assertTrue(Files.exists(backupDir.resolve(".git"))); // Ensure the repo exists

        // Use backupGitDiffers to check if the backup differs
        boolean createBackup;
        if (bibDatabaseContext.getDatabasePath().isPresent()) {
            createBackup = BackupManagerGit.backupGitDiffers(bibDatabaseContext.getDatabasePath().get(), backupDir);
        } else {
            fail("Database path is not present");
            return; // Avoid further execution if the path is missing
        }

        // Shutdown the initial manager
        BackupManagerGit.shutdown(bibDatabaseContext, backupDir, createBackup, bibDatabaseContext.getDatabasePath().get());

        // Create another instance pointing to the same backup directory
        BackupManagerGit newManager = new BackupManagerGit(libraryTab, bibDatabaseContext, entryTypesManager, preferences);
        assertTrue(Files.exists(backupDir.resolve(".git"))); // Ensure no new repo is created

        // Shutdown the new manager
        BackupManagerGit.shutdown(bibDatabaseContext, backupDir, createBackup, bibDatabaseContext.getDatabasePath().get());
    }
}







