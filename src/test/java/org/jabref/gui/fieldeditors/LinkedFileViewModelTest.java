package org.jabref.gui.fieldeditors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.FileDirectoryPreferences;
import org.jabref.preferences.JabRefPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(TempDirectory.class)
/**
 * Must be public because otherwise @TestArchitectureTests fails!
 *
*/
public class LinkedFileViewModelTest {

    private Path tempFile;
    private final JabRefPreferences preferences = mock(JabRefPreferences.class);
    private LinkedFile linkedFile;
    private BibEntry entry;
    private BibDatabaseContext databaseContext;
    private TaskExecutor taskExecutor;
    private DialogService dialogService;
    private final FileDirectoryPreferences fileDirectoryPreferences = mock(FileDirectoryPreferences.class);

    @BeforeEach
    void setUp(@TempDirectory.TempDir Path tempFolder) throws Exception {
        entry = new BibEntry();
        databaseContext = new BibDatabaseContext();
        taskExecutor = mock(TaskExecutor.class);
        dialogService = mock(DialogService.class);

        tempFile = tempFolder.resolve("temporaryFile");
        Files.createFile(tempFile);
    }

    @Test
    void deleteWhenFilePathNotPresentReturnsTrue() {
        // Making this a spy, so we can inject an empty optional without digging into the implementation
        linkedFile = spy(new LinkedFile("", "nonexistent file", ""));
        doReturn(Optional.empty()).when(linkedFile).findIn(any(BibDatabaseContext.class), any(FileDirectoryPreferences.class));

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);
        boolean removed = viewModel.delete(fileDirectoryPreferences);

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
        boolean removed = viewModel.delete(fileDirectoryPreferences);

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
        boolean removed = viewModel.delete(fileDirectoryPreferences);

        assertTrue(removed);
        assertFalse(Files.exists(tempFile));
    }

    @Test
    void deleteWhenDeleteChosenAndFileMissingReturnsFalse() {
        linkedFile = new LinkedFile("", "!!nonexistent file!!", "");
        when(dialogService.showCustomButtonDialogAndWait(
                any(AlertType.class),
                anyString(),
                anyString(),
                any(ButtonType.class),
                any(ButtonType.class),
                any(ButtonType.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(4))); // second vararg - delete button

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);
        boolean removed = viewModel.delete(fileDirectoryPreferences);

        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
        assertFalse(removed);
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
        boolean removed = viewModel.delete(fileDirectoryPreferences);

        assertFalse(removed);
        assertTrue(Files.exists(tempFile));
    }
}
