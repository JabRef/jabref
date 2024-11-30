package org.jabref.gui.autosaveandbackup;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.backup.BackupEntry;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        // Initialize the Git repository inside backupDir
        git = Git.init().setDirectory(backupDir.toFile()).call();

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

    @Test
    void testStartMethod() throws Exception {
        // Arrange: Set up necessary dependencies and mock objects
        Path databaseFile = tempDir.resolve("test.bib");
        Files.writeString(databaseFile, "Initial content");

        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase());
        bibDatabaseContext.setDatabasePath(databaseFile);

        Path backupDirectory = tempDir.resolve("backup");
        Files.createDirectories(backupDirectory);

        // Mock preferences to return the backup directory
        when(preferences.getFilePreferences().getBackupDirectory()).thenReturn(backupDirectory);

        // Act: Call the start method
        BackupManagerGit backupManager = BackupManagerGit.start(
                libraryTab,
                bibDatabaseContext,
                entryTypesManager,
                preferences,
                databaseFile
        );

        // Assert: Verify the outcomes
        // Ensure a Git repository is initialized in the backup directory
        assertTrue(Files.exists(backupDirectory.resolve(".git")), "Git repository not initialized");

        // Use reflection to access the private `runningInstances`
        Field runningInstancesField = BackupManagerGit.class.getDeclaredField("runningInstances");
        runningInstancesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<BackupManagerGit> runningInstances = (Set<BackupManagerGit>) runningInstancesField.get(null);

        // Ensure the backup manager is added to the running instances
        assertTrue(runningInstances.contains(backupManager), "Backup manager not added to running instances");

        // Clean up by shutting down the backup manager
        BackupManagerGit.shutdown(bibDatabaseContext, backupDirectory, false, databaseFile);
    }

    @Test
    void testStartBackupTaskWithReflection() throws Exception {
        // Arrange: Similar setup as above
        Path databaseFile = tempDir.resolve("test.bib");
        Files.writeString(databaseFile, "Initial content");

        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase());
        bibDatabaseContext.setDatabasePath(databaseFile);

        Path backupDirectory = tempDir.resolve("backup");
        Files.createDirectories(backupDirectory);

        when(preferences.getFilePreferences().getBackupDirectory()).thenReturn(backupDirectory);

        BackupManagerGit backupManager = BackupManagerGit.start(
                libraryTab,
                bibDatabaseContext,
                entryTypesManager,
                preferences,
                databaseFile
        );

        // Act: Start the backup task
        // private void startBackupTask(Path backupDir, Path originalPath)
        backupManager.startBackupTask(backupDirectory, databaseFile);

        // Simulate passage of time
        Thread.sleep(100);

        // Use reflection to access the private `runningInstances`
        Field runningInstancesField = BackupManagerGit.class.getDeclaredField("runningInstances");
        runningInstancesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<BackupManagerGit> runningInstances = (Set<BackupManagerGit>) runningInstancesField.get(null);

        // Assert: Verify the backup task is active
        assertTrue(runningInstances.contains(backupManager), "Backup manager not added to running instances");

        // Clean up
        BackupManagerGit.shutdown(bibDatabaseContext, backupDirectory, false, databaseFile);
    }

    @Test
    void testRestoreBackup() throws Exception {
        // Create multiple commits
        ObjectId targetCommitId = null;
        for (int i = 1; i <= 3; i++) {
            Path file = backupDir.resolve("file" + i + ".txt");
            Files.writeString(file, "Content of file " + i);
            git.add().addFilepattern(".").call();
            RevCommit commit = git.commit().setMessage("Commit " + i).call();
            if (i == 2) {
                // Save the ID of the second commit for testing
                targetCommitId = commit.getId();
            }
        }

        // Act: Call restoreBackup
        BackupManagerGit.restoreBackup(tempDir.resolve("restored.txt"), backupDir, targetCommitId);

        // Assert: Verify the repository has a new commit after restoration
        try (RevWalk revWalk = new RevWalk(git.getRepository())) {
            RevCommit headCommit = revWalk.parseCommit(git.getRepository().resolve("HEAD"));
            assertTrue(
                    headCommit.getShortMessage().contains("Restored content from commit: " + targetCommitId.getName()),
                    "A new commit should indicate the restoration"
            );
        }

        // Assert: Ensure the file from the restored commit exists
        assertTrue(
                Files.exists(backupDir.resolve("file2.txt")),
                "File from the restored commit should be present"
        );

        // Assert: Ensure files from later commits still exist
        assertTrue(
                Files.exists(backupDir.resolve("file3.txt")),
                "File from later commits should still exist after restoration"
        );

        // Assert: Ensure earlier files still exist
        assertTrue(
                Files.exists(backupDir.resolve("file1.txt")),
                "File from earlier commits should still exist after restoration"
        );
    }

    @Test
    void testRetrieveCommits() throws Exception {
        // Create multiple commits in the Git repository
        List<ObjectId> commitIds = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Path file = backupDir.resolve("file" + i + ".txt");
            Files.writeString(file, "Content of file " + i);
            git.add().addFilepattern(".").call();
            RevCommit commit = git.commit().setMessage("Commit " + i).call();
            commitIds.add(commit.getId());
        }

        // Act: Call retrieveCommits to get the last 5 commits
        // Arrange: Similar setup as above
        Path databaseFile = tempDir.resolve("test.bib");
        Files.writeString(databaseFile, "Initial content");

        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase());
        bibDatabaseContext.setDatabasePath(databaseFile);

        Path backupDirectory = tempDir.resolve("backup");
        Files.createDirectories(backupDirectory);

        when(preferences.getFilePreferences().getBackupDirectory()).thenReturn(backupDirectory);

        BackupManagerGit backupManager = BackupManagerGit.start(
                libraryTab,
                bibDatabaseContext,
                entryTypesManager,
                preferences,
                databaseFile
        );

        List<RevCommit> retrievedCommits = backupManager.retrieveCommits(backupDir, 5);

        // Assert: Verify the number of commits retrieved
        assertEquals(5, retrievedCommits.size(), "Should retrieve the last 5 commits");

        // Assert: Verify the content of the retrieved commits
        for (int i = 0; i < 5; i++) {
            RevCommit retrievedCommit = retrievedCommits.get(i);
            int finalI = i;
            RevCommit expectedCommit = StreamSupport.stream(git.log().call().spliterator(), false)
                                                    .filter(commit -> commit.getId().equals(commitIds.get(commitIds.size() - 5 + finalI)))
                                                    .findFirst()
                                                    .orElse(null);

            assertNotNull(expectedCommit, "Expected commit should exist in the repository");
            assertEquals(expectedCommit.getFullMessage(), retrievedCommit.getFullMessage(),
                    "Commit messages should match");
            assertEquals(expectedCommit.getId(), retrievedCommit.getId(),
                    "Commit IDs should match");
        }
    }

    @Test
    void testRetrieveCommitDetails() throws Exception {
        // Create multiple commits in the Git repository
        List<RevCommit> commits = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Path file = backupDir.resolve("file" + i + ".txt");
            Files.writeString(file, "Content of file " + i);
            git.add().addFilepattern(".").call();
            RevCommit commit = git.commit().setMessage("Commit " + i).call();
            commits.add(commit);
        }

        // Act: Call retrieveCommitDetails to get the details of the commits
        // Arrange: Similar setup as above
        Path databaseFile = tempDir.resolve("test.bib");
        Files.writeString(databaseFile, "Initial content");

        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase());
        bibDatabaseContext.setDatabasePath(databaseFile);

        Path backupDirectory = tempDir.resolve("backup");
        Files.createDirectories(backupDirectory);

        when(preferences.getFilePreferences().getBackupDirectory()).thenReturn(backupDirectory);

        BackupManagerGit backupManager = BackupManagerGit.start(
                libraryTab,
                bibDatabaseContext,
                entryTypesManager,
                preferences,
                databaseFile
        );
        List<BackupEntry> commitDetails = backupManager.retrieveCommitDetails(commits, backupDir);

        // Assert: Verify the number of commits
        assertEquals(5, commitDetails.size(), "Should retrieve details for 5 commits");

        // Assert: Verify the content of the retrieved commit details
        for (int i = 0; i < 5; i++) {
            BackupEntry commitInfo = commitDetails.get(i);
            RevCommit commit = commits.get(i);

            // Verify commit ID
            assertEquals(commit.getName(), commitInfo.getName(), "Commit ID should match");

            // Verify commit size (this is a bit tricky, so just check it's a valid size string)
            String sizeFormatted = commitInfo.getSize();
            assertTrue(sizeFormatted.contains("Ko") || sizeFormatted.contains("Mo"), "Commit size should be properly formatted");

            // Verify commit date
            String commitDate = commitInfo.getDate();
            assertTrue(commitDate.contains(commit.getAuthorIdent().getWhen().toString()), "Commit date should match");
        }
    }
}







