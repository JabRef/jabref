package org.jabref.gui.autosaveandbackup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.gui.LibraryTab;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackupManagerGitTest {

    private Path backupDir;
    private BackupManagerGit backupManager;

    @BeforeEach
    void setup(@TempDir Path tempDir) throws IOException, GitAPIException, IOException {
        backupDir = tempDir.resolve("backup");
        Files.createDirectories(backupDir);

        // Initialize BackupManagerGit with mock dependencies
        var libraryTab = mock(LibraryTab.class);
        var bibDatabaseContext = new BibDatabaseContext();
        var entryTypesManager = mock(BibEntryTypesManager.class);
        var preferences = mock(CliPreferences.class);
        var filePreferences = mock(FilePreferences.class);

        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(filePreferences.getBackupDirectory()).thenReturn(backupDir);

        backupManager = new BackupManagerGit(libraryTab, bibDatabaseContext, entryTypesManager, preferences);
    }

    @Test
    void testInitialization() {
        assertNotNull(backupManager);
    }
}




