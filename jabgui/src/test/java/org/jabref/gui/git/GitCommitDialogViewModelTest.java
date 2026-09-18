package org.jabref.gui.git;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GitCommitDialogViewModelTest {

    @Test
    void generateCommitMessageReportsAddedEntries() {
        BibEntry entry = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "Test Entry");

        BibDatabaseContext headDatabase = new BibDatabaseContext(new BibDatabase());

        BibDatabaseContext workingTreeDatabase = new BibDatabaseContext(new BibDatabase(List.of(entry)));

        GitCommitDialogViewModel.DiffDatabases diff = new GitCommitDialogViewModel.DiffDatabases(headDatabase, workingTreeDatabase);

        GitCommitDialogViewModel viewModel = new GitCommitDialogViewModel(mock(StateManager.class), mock(DialogService.class), mock(TaskExecutor.class), mock(GitHandlerRegistry.class), mock(ImportFormatPreferences.class), mock(FileUpdateMonitor.class));

        assertEquals("Add 1 entry", viewModel.generateCommitMessage(diff));
    }

    @Test
    void generateCommitMessageReportsDeletedEntries() {
        BibEntry entry = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "Test Entry");

        BibDatabaseContext headDatabase = new BibDatabaseContext(new BibDatabase(List.of(entry)));

        BibDatabaseContext workingTreeDatabase = new BibDatabaseContext(new BibDatabase());

        GitCommitDialogViewModel.DiffDatabases diff = new GitCommitDialogViewModel.DiffDatabases(headDatabase, workingTreeDatabase);

        GitCommitDialogViewModel viewModel = new GitCommitDialogViewModel(mock(StateManager.class), mock(DialogService.class), mock(TaskExecutor.class), mock(GitHandlerRegistry.class), mock(ImportFormatPreferences.class), mock(FileUpdateMonitor.class));

        assertEquals("Delete 1 entry", viewModel.generateCommitMessage(diff));
    }

    @Test
    void generateCommitMessageReportsModifiedEntries() {
        BibEntry headEntry = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "Old Title").withCitationKey("key");

        BibEntry workingTreeEntry = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "New Title").withCitationKey("key");

        BibDatabaseContext headDatabase = new BibDatabaseContext(new BibDatabase(List.of(headEntry)));

        BibDatabaseContext workingTreeDatabase = new BibDatabaseContext(new BibDatabase(List.of(workingTreeEntry)));

        GitCommitDialogViewModel.DiffDatabases diff = new GitCommitDialogViewModel.DiffDatabases(headDatabase, workingTreeDatabase);

        GitCommitDialogViewModel viewModel = new GitCommitDialogViewModel(mock(StateManager.class), mock(DialogService.class), mock(TaskExecutor.class), mock(GitHandlerRegistry.class), mock(ImportFormatPreferences.class), mock(FileUpdateMonitor.class));

        assertEquals("Modify 1 entry", viewModel.generateCommitMessage(diff));
    }
}
