
package org.jabref.gui.autosaveandbackup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.gui.LibraryTab;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.MetaData;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BackupManagerTest {

    private Path tempDir1;
    private Path tempDir2;
    private Path tempDir;
    private LibraryTab mockLibraryTab;
    private BibDatabaseContext mockDatabaseContext1;
    private BibDatabaseContext mockDatabaseContext2;
    private BibEntryTypesManager mockEntryTypesManager;
    private CliPreferences mockPreferences;
    private Path dataBasePath1;
    private Path dataBasePath2;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws IOException, GitAPIException {

        // creating Entries
        BibEntry entry1 = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Garcia, Maria and Lee, David")
                .withField(StandardField.JOURNAL, "International Review of Physics")
                .withField(StandardField.NUMBER, "6")
                .withField(StandardField.PAGES, "789--810")
                .withField(StandardField.TITLE, "Quantum Entanglement in Superconductors")
                .withField(StandardField.VOLUME, "28")
                .withField(StandardField.ISSUE, "3")
                .withField(StandardField.YEAR, "2021")
                .withCitationKey("Garcia_2021");
        BibEntry entry2 = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.AUTHOR, "Smith, John")
                .withField(StandardField.TITLE, "Advanced Quantum Mechanics")
                .withField(StandardField.PUBLISHER, "Physics Press")
                .withField(StandardField.YEAR, "2019")
                .withField(StandardField.ISBN, "978-3-16-148410-0")
                .withCitationKey("Smith_2019");

        BibEntry entry3 = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "Doe, Jane and Brown, Alice")
                .withField(StandardField.TITLE, "Machine Learning in Quantum Computing")
                .withField(StandardField.BOOKTITLE, "Proceedings of the International Conference on Quantum Computing")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.PAGES, "123-130")
                .withCitationKey("Doe_2020");

        BibEntry entry4 = new BibEntry(StandardEntryType.Thesis)
                .withField(StandardField.AUTHOR, "Johnson, Emily")
                .withField(StandardField.TITLE, "Quantum Algorithms for Data Analysis")
                .withField(StandardField.SCHOOL, "University of Quantum Studies")
                .withField(StandardField.YEAR, "2022")
                .withField(StandardField.TYPE, "PhD Thesis")
                .withCitationKey("Johnson_2022");

        List<BibEntry> entries1 = new ArrayList<>();
        entries1.add(entry1);
        entries1.add(entry2);
        List<BibEntry> entries2 = new ArrayList<>();
        entries2.add(entry3);
        entries2.add(entry4);

        // Initializing BibDatabases
        BibDatabase bibDatabase1 = new BibDatabase(entries1);
        BibDatabase bibDatabase2 = new BibDatabase(entries2);

        // Create temporary backup directories and .bib files
        this.tempDir = tempDir.resolve("");
        this.tempDir1 = tempDir.resolve("backup1");
        this.tempDir2 = tempDir.resolve("backup2");
        dataBasePath1 = tempDir1.resolve("test1.bib");
        dataBasePath2 = tempDir2.resolve("test2.bib");

        // Ensure the directories exists
        Files.createDirectories(this.tempDir);
        Files.createDirectories(this.tempDir1);
        Files.createDirectories(this.tempDir2);

        // creating the bibDatabaseContexts
        mockDatabaseContext1 = new BibDatabaseContext(bibDatabase1, new MetaData(), dataBasePath1);
        mockDatabaseContext2 = new BibDatabaseContext(bibDatabase2, new MetaData(), dataBasePath2);
        mockEntryTypesManager = mock(BibEntryTypesManager.class);

        // creating the mockLibraryTab
        mockLibraryTab = mock(LibraryTab.class);

        // creating the mockPreferences
        mockPreferences = mock(CliPreferences.class);
        FilePreferences filePreferences = mock(FilePreferences.class);
        when(mockPreferences.getFilePreferences()).thenReturn(filePreferences);
        when(filePreferences.getBackupDirectory()).thenReturn(tempDir);

        // creating the content of the .bib files
        Files.writeString(dataBasePath1, "Mock content for testing 1"); // Create the file
        Files.writeString(dataBasePath2, "Mock content for testing 2"); // Create the file
    }

    @Test
    void initializationCreatesBackupDirectory() throws IOException, GitAPIException {
        // Create BackupManagerGit
        BackupManager manager1 = new BackupManager(mockLibraryTab, mockDatabaseContext1, mockEntryTypesManager, tempDir);
        BackupManager manager2 = new BackupManager(mockLibraryTab, mockDatabaseContext2, mockEntryTypesManager, tempDir);
        // Check if the backup directory exists
        assertTrue(Files.exists(tempDir), " directory should be created which contains .git and single copies og .bib");
        assertTrue(Files.exists(tempDir1), "Backup directory should be created during initialization.");
        assertTrue(Files.exists(tempDir2), "Backup directory should be created during initialization.");
    }

    @Test
    void gitInitialization() throws IOException, GitAPIException {
        // Initialize Git
        BackupManager.ensureGitInitialized(tempDir);
        // Verify that the .git directory is created
        Path gitDir = tempDir.resolve(".git");
        assertTrue(Files.exists(gitDir), ".git directory should be created during Git initialization.");
    }

    @Test
    void backupFileCopiedToDirectory() throws IOException, GitAPIException {
        BackupManager manager1 = new BackupManager(mockLibraryTab, mockDatabaseContext1, mockEntryTypesManager, tempDir);
        BackupManager manager2 = new BackupManager(mockLibraryTab, mockDatabaseContext2, mockEntryTypesManager, tempDir);

        // Generate the expected backup file names
        String uuid1 = BackupManager.getOrGenerateFileUuid(dataBasePath1);
        String uuid2 = BackupManager.getOrGenerateFileUuid(dataBasePath2);

        String backupFileName1 = dataBasePath1.getFileName().toString().replace(".bib", "") + "_" + uuid1 + ".bib";
        String backupFileName2 = dataBasePath2.getFileName().toString().replace(".bib", "") + "_" + uuid2 + ".bib";

        // Verify the file is copied to the backup directory
        Path backupFile1 = tempDir.resolve(backupFileName1);
        Path backupFile2 = tempDir.resolve(backupFileName2);
        assertTrue(Files.exists(backupFile1), "Database file should be copied to the backup directory.");
        assertTrue(Files.exists(backupFile2), "Database file should be copied to the backup directory.");
    }

    @Test
    public void start() throws IOException, GitAPIException {
        BackupManager startedManager = BackupManager.start(mockLibraryTab, mockDatabaseContext1, mockEntryTypesManager, mockPreferences);
        assertNotNull(startedManager);
    }

    @Test
    void performBackupCommitsChanges() throws IOException, GitAPIException {
        // Initialize Git
        BackupManager.ensureGitInitialized(tempDir);

        // Create a test file
        Path dbFile1 = tempDir.resolve("test1.bib");

        // Create BackupManagerGit and perform backup
        BackupManager manager = new BackupManager(mockLibraryTab, mockDatabaseContext1, mockEntryTypesManager, tempDir);
        Files.writeString(dbFile1, "Initial content of test 1");

        BackupManager.copyDatabaseFileToBackupDir(dbFile1, tempDir);

        // Generate the expected backup file name
        String uuid1 = BackupManager.getOrGenerateFileUuid(dbFile1);
        String backupFileName1 = dbFile1.getFileName().toString().replace(".bib", "") + "_" + uuid1 + ".bib";
        Path backupFile1 = tempDir.resolve(backupFileName1);

        // Verify the file is copied to the backup directory
        assertTrue(Files.exists(backupFile1), "Database file should be copied to the backup directory.");

        manager.performBackup(dbFile1, tempDir);

        // Verify that changes are committed
        try (Git git = Git.open(tempDir.toFile())) {
            boolean hasUncommittedChanges = git.status().call().getUncommittedChanges().stream()
                                               .anyMatch(file -> file.endsWith(".bib"));
            assertFalse(hasUncommittedChanges, "Git repository should have no uncommitted .bib file changes after backup.");
        }
    }
}
