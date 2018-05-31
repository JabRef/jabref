package org.jabref.gui.fieldeditors;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.FileDirectoryPreferences;
import org.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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

public class LinkedFileViewModelTest {

    @Rule public TemporaryFolder tempFolder = new TemporaryFolder();
    private final JabRefPreferences preferences = mock(JabRefPreferences.class);
    private final JournalAbbreviationLoader abbreviationLoader = mock(JournalAbbreviationLoader.class);
    private LinkedFile linkedFile;
    private BibEntry entry;
    private BibDatabaseContext databaseContext;
    private TaskExecutor taskExecutor;
    private DialogService dialogService;
    private final FileDirectoryPreferences fileDirectoryPreferences = mock(FileDirectoryPreferences.class);

    @Before
    public void setUp() {
        entry = new BibEntry();
        databaseContext = new BibDatabaseContext();
        taskExecutor = mock(TaskExecutor.class);
        dialogService = mock(DialogService.class);
        Globals.prefs = mock(JabRefPreferences.class);
        when(Globals.prefs.getDefaultEncoding()).thenReturn(StandardCharsets.UTF_8);
        FileDirectoryPreferences fileDirectoryPreferences = mock(FileDirectoryPreferences.class);
        when(Globals.prefs.getFileDirectoryPreferences()).thenReturn(fileDirectoryPreferences);
    }

    @Test
    public void deleteWhenFilePathNotPresentReturnsTrue() {
        // Making this a spy, so we can inject an empty optional without digging into the implementation
        linkedFile = spy(new LinkedFile("", "nonexistent file", ""));
        doReturn(Optional.empty()).when(linkedFile).findIn(any(BibDatabaseContext.class), any(FileDirectoryPreferences.class));

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences, abbreviationLoader);
        boolean removed = viewModel.delete(fileDirectoryPreferences);

        assertTrue(removed);
        verifyZeroInteractions(dialogService); // dialog was never shown
    }

    @Test
    public void deleteWhenRemoveChosenReturnsTrue() throws IOException {
        File tempFile = tempFolder.newFile();
        linkedFile = new LinkedFile("", tempFile.getAbsolutePath(), "");
        when(dialogService.showCustomButtonDialogAndWait(
                any(AlertType.class),
                anyString(),
                anyString(),
                any(ButtonType.class),
                any(ButtonType.class),
                any(ButtonType.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(3))); // first vararg - remove button

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences, abbreviationLoader);
        boolean removed = viewModel.delete(fileDirectoryPreferences);

        assertTrue(removed);
        assertTrue(tempFile.exists());
    }

    @Test
    public void deleteWhenDeleteChosenReturnsTrueAndDeletesFile() throws IOException {
        File tempFile = tempFolder.newFile();
        linkedFile = new LinkedFile("", tempFile.getAbsolutePath(), "");
        when(dialogService.showCustomButtonDialogAndWait(
                any(AlertType.class),
                anyString(),
                anyString(),
                any(ButtonType.class),
                any(ButtonType.class),
                any(ButtonType.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(4))); // second vararg - delete button

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences, abbreviationLoader);
        boolean removed = viewModel.delete(fileDirectoryPreferences);

        assertTrue(removed);
        assertFalse(tempFile.exists());
    }

    @Test
    public void deleteWhenDeleteChosenAndFileMissingReturnsFalse() throws IOException {
        linkedFile = new LinkedFile("", "!!nonexistent file!!", "");
        when(dialogService.showCustomButtonDialogAndWait(
                any(AlertType.class),
                anyString(),
                anyString(),
                any(ButtonType.class),
                any(ButtonType.class),
                any(ButtonType.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(4))); // second vararg - delete button

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences, abbreviationLoader);
        boolean removed = viewModel.delete(fileDirectoryPreferences);

        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
        assertFalse(removed);
    }

    @Test
    public void deleteWhenDialogCancelledReturnsFalse() throws IOException {
        File tempFile = tempFolder.newFile();
        linkedFile = new LinkedFile("desc", tempFile.getAbsolutePath(), "pdf");
        when(dialogService.showCustomButtonDialogAndWait(
                any(AlertType.class),
                anyString(),
                anyString(),
                any(ButtonType.class),
                any(ButtonType.class),
                any(ButtonType.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(5))); // third vararg - cancel button

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences, abbreviationLoader);
        boolean removed = viewModel.delete(fileDirectoryPreferences);

        assertFalse(removed);
        assertTrue(tempFile.exists());
    }
}
