package org.jabref.gui.fieldeditors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Map;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.externalfiles.LinkedFileHandler;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.FilePreferences;
import org.jabref.preferences.JabRefPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.mockito.Answers;
import org.mockito.internal.stubbing.BaseStubbing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(TempDirectory.class)
class LinkedFileViewModelTest {

    private Path tempFile;
    private final JabRefPreferences preferences = mock(JabRefPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private LinkedFile linkedFile;
    private BibEntry entry;
    private BibDatabaseContext databaseContext;
    private TaskExecutor taskExecutor;
    private DialogService dialogService;

    @BeforeEach
    void setUp(@TempDirectory.TempDir Path tempFolder) throws Exception {
        entry = new BibEntry();
        databaseContext = new BibDatabaseContext();
        taskExecutor = mock(TaskExecutor.class);
        dialogService = mock(DialogService.class);

        tempFile = tempFolder.resolve("temporaryFile");
        Files.createFile(tempFile);
    }

    //In-Progress Implementation for CheckNameConflict
    @Test
    void checkNameConflict() {
        //Debug Statements
        System.out.println(tempFile.getFileName());
        System.out.println(tempFile.toAbsolutePath().toString());

        linkedFile = spy(new LinkedFile("", tempFile.toString(), ""));
        doReturn(Optional.empty()).when(linkedFile).findIn(any(BibDatabaseContext.class), any(FilePreferences.class));
        //when(preferences.getFilePreferences()).thenReturn(new FilePreferences(eq("TestUser"), anyMap(), eq(true), eq("TestPattern"), eq("TestDirectoryPattern")));

        //Debug Statements
        System.out.println(linkedFile.toString());
        System.out.println(linkedFile.getLink());

        LinkedFileViewModel viewModel = spy(new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences));
        //when(FileUtil.createFileNameFromPattern(any(BibDatabase.class), any(BibEntry.class), any(String.class))).thenReturn("TestFileName.txt");
        when(new LinkedFileHandler(this.linkedFile, this.entry, this.databaseContext, this.preferences.getFilePreferences()).getSuggestedFileName()).thenReturn("temporaryFile");

        boolean equalNames = viewModel.performNameConflictCheck();

        assertTrue(!equalNames);
    }

    @Test
    void deleteWhenFilePathNotPresentReturnsTrue() {
        // Making this a spy, so we can inject an empty optional without digging into the implementation
        linkedFile = spy(new LinkedFile("", "nonexistent file", ""));
        doReturn(Optional.empty()).when(linkedFile).findIn(any(BibDatabaseContext.class), any(FilePreferences.class));

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);
        boolean removed = viewModel.delete();

        assertTrue(removed);
        verifyZeroInteractions(dialogService); // dialog was never shown
    }

    @Test
    void deleteWhenRemoveChosenReturnsTrueButDoesNotDeletesFile() {
        linkedFile = new LinkedFile("", tempFile.toString(), "");
        when(dialogService.showCustomButtonDialogAndWait(
                any(AlertType.class),
                anyString(),
                anyString(),
                any(ButtonType.class),
                any(ButtonType.class),
                any(ButtonType.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(3))); // first vararg - remove button

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);
        boolean removed = viewModel.delete();

        assertTrue(removed);
        assertTrue(Files.exists(tempFile));
    }

    @Test
    void deleteWhenDeleteChosenReturnsTrueAndDeletesFile() {
        linkedFile = new LinkedFile("", tempFile.toString(), "");
        when(dialogService.showCustomButtonDialogAndWait(
                any(AlertType.class),
                anyString(),
                anyString(),
                any(ButtonType.class),
                any(ButtonType.class),
                any(ButtonType.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(4))); // second vararg - delete button

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);
        boolean removed = viewModel.delete();

        assertTrue(removed);
        assertFalse(Files.exists(tempFile));
    }

    @Test
    void deleteMissingFileReturnsTrue() {
        linkedFile = new LinkedFile("", "!!nonexistent file!!", "");
        when(dialogService.showCustomButtonDialogAndWait(
                any(AlertType.class),
                anyString(),
                anyString(),
                any(ButtonType.class),
                any(ButtonType.class),
                any(ButtonType.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(4))); // second vararg - delete button

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);
        boolean removed = viewModel.delete();

        assertTrue(removed);
    }

    @Test
    void deleteWhenDialogCancelledReturnsFalseAndDoesNotRemoveFile() {
        linkedFile = new LinkedFile("desc", tempFile.toString(), "pdf");
        when(dialogService.showCustomButtonDialogAndWait(
                any(AlertType.class),
                anyString(),
                anyString(),
                any(ButtonType.class),
                any(ButtonType.class),
                any(ButtonType.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(5))); // third vararg - cancel button

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);
        boolean removed = viewModel.delete();

        assertFalse(removed);
        assertTrue(Files.exists(tempFile));
    }
}
