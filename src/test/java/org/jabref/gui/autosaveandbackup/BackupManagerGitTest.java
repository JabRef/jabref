
package org.jabref.gui.autosaveandbackup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.gui.LibraryTab;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.metadata.MetaData;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BackupManagerGitTest {

    private Path tempDir1;
    private Path tempDir2;
    private Path tempDir;
    private LibraryTab mockLibraryTab;
    private BibDatabaseContext mockDatabaseContext1;
    private BibDatabaseContext mockDatabaseContext2;
    private BibEntryTypesManager mockEntryTypesManager;
    private CliPreferences mockPreferences;
    private Path mockDatabasePath1;
    private Path mockDatabasePath2;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws IOException, GitAPIException {
        mockLibraryTab = mock(LibraryTab.class);
        mockDatabaseContext1 = mock(BibDatabaseContext.class);
        mockDatabaseContext2 = mock(BibDatabaseContext.class);
        mockEntryTypesManager = mock(BibEntryTypesManager.class);
        mockPreferences = mock(CliPreferences.class);

        FilePreferences filePreferences = mock(FilePreferences.class);
        when(mockPreferences.getFilePreferences()).thenReturn(filePreferences);

        // Create temporary backup directories

        this.tempDir = tempDir.resolve("");
        this.tempDir1 = tempDir.resolve("backup1");
        this.tempDir2 = tempDir.resolve("backup2");
        Files.createDirectories(this.tempDir); // Ensure the directory exists
        Files.createDirectories(this.tempDir1); // Ensure the directory exists
        Files.createDirectories(this.tempDir2); // Ensure the directory exists

        // Mock the database paths and create the actual files
         mockDatabasePath1 = tempDir1.resolve("test1.bib");
         mockDatabasePath2 = tempDir2.resolve("test2.bib");

        Files.writeString(mockDatabasePath1, "Mock content for testing 1"); // Create the file
        Files.writeString(mockDatabasePath2, "Mock content for testing 2"); // Create the file

        when(mockDatabaseContext1.getDatabasePath()).thenReturn(java.util.Optional.of(mockDatabasePath1));
        when(mockDatabaseContext2.getDatabasePath()).thenReturn(java.util.Optional.of(mockDatabasePath2));

        when(filePreferences.getBackupDirectory()).thenReturn(tempDir);

        // Mock BibDatabase for all contexts
        BibDatabase mockDatabase1 = mock(BibDatabase.class);
        when(mockDatabaseContext1.getDatabase()).thenReturn(mockDatabase1);

        BibDatabase mockDatabase2 = mock(BibDatabase.class);
        when(mockDatabaseContext2.getDatabase()).thenReturn(mockDatabase2);

        // Mock MetaData for all contexts (if needed elsewhere)
        MetaData mockMetaData1 = mock(MetaData.class);
        when(mockDatabaseContext1.getMetaData()).thenReturn(mockMetaData1);

        MetaData mockMetaData2 = mock(MetaData.class);
        when(mockDatabaseContext2.getMetaData()).thenReturn(mockMetaData2);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Delete the temporary directory
        Files.walk(tempDir)
             .map(Path::toFile)
             .forEach(file -> {
                 if (!file.delete()) {
                     file.deleteOnExit();
                 }
             });
    }

    @Test
    void testInitializationCreatesBackupDirectory() throws IOException, GitAPIException {
        // Create BackupManagerGit
        BackupManagerGit manager1 = new BackupManagerGit(mockLibraryTab, mockDatabaseContext1, mockEntryTypesManager, mockPreferences);
        BackupManagerGit manager2 = new BackupManagerGit(mockLibraryTab, mockDatabaseContext2, mockEntryTypesManager, mockPreferences);
        // Check if the backup directory exists
        assertTrue(Files.exists(tempDir), " directory should be created wich contains .git and single copies og .bib");
        assertTrue(Files.exists(tempDir1), "Backup directory should be created during initialization.");
        assertTrue(Files.exists(tempDir2), "Backup directory should be created during initialization.");
    }

    @Test
    void testGitInitialization() throws IOException, GitAPIException {
        // Initialize Git
        BackupManagerGit.ensureGitInitialized(tempDir);

        // Verify that the .git directory is created
        Path gitDir = tempDir.resolve(".git");
        assertTrue(Files.exists(gitDir), ".git directory should be created during Git initialization.");
    }

    @Test
    void testBackupFileCopiedToDirectory() throws IOException, GitAPIException {

        BackupManagerGit manager1 = new BackupManagerGit(mockLibraryTab, mockDatabaseContext1, mockEntryTypesManager, mockPreferences);
        BackupManagerGit manager2 = new BackupManagerGit(mockLibraryTab, mockDatabaseContext2, mockEntryTypesManager, mockPreferences);

        // Verify the file is copied to the backup directory
        Path backupFile1 = tempDir.resolve(this.mockDatabasePath1.getFileName());
        Path backupFile2 = tempDir.resolve(this.mockDatabasePath2.getFileName());
        assertTrue(Files.exists(backupFile1), "Database file should be copied to the backup directory.");
        }

    @Test
    public void testStart() throws IOException, GitAPIException {
        BackupManagerGit startedManager = BackupManagerGit.start(mockLibraryTab, mockDatabaseContext1, mockEntryTypesManager, mockPreferences);
        assertNotNull(startedManager);
    }

    @Test
    void testPerformBackupCommitsChanges() throws IOException, GitAPIException {
        // Initialize Git
        BackupManagerGit.ensureGitInitialized(tempDir);

        // Create a test file
        Path dbFile1 = tempDir.resolve("test1.bib");
        Files.writeString(dbFile1, "Initial content of test 1");

        // Create BackupManagerGit and perform backup
        BackupManagerGit manager = new BackupManagerGit(mockLibraryTab, mockDatabaseContext1, mockEntryTypesManager, mockPreferences);
        manager.performBackup(tempDir);

        // Verify that changes are committed
        try (Git git = Git.open(tempDir.toFile())) {
            assertTrue(git.status().call().isClean(), "Git repository should have no uncommitted changes after backup.");
        }
    }
}
