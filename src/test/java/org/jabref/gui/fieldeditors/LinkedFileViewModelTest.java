package org.jabref.gui.fieldeditors;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.TreeSet;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.StandardExternalFileType;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.externalfiles.LinkedFileHandler;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.metadata.FilePreferences;
import org.jabref.preferences.JabRefPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

class LinkedFileViewModelTest {

    private Path tempFile;
    private final JabRefPreferences preferences = mock(JabRefPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private LinkedFile linkedFile;
    private BibEntry entry;
    private BibDatabaseContext databaseContext;
    private TaskExecutor taskExecutor;
    private DialogService dialogService;
    private ExternalFileTypes externalFileType = mock(ExternalFileTypes.class);
    private FilePreferences filePreferences = mock(FilePreferences.class);
    private LinkedFileHandler linkedFileHandler = mock(LinkedFileHandler.class);

    @BeforeEach
    void setUp(@TempDir Path tempFolder) throws Exception {
        entry = new BibEntry();
        entry.setCiteKey("asdf");
        databaseContext = new BibDatabaseContext();
        taskExecutor = mock(TaskExecutor.class);
        dialogService = mock(DialogService.class);

        when(externalFileType.getExternalFileTypeSelection()).thenReturn(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes()));
        when(externalFileType.getExternalFileTypeByMimeType("application/pdf")).thenReturn(Optional.of(StandardExternalFileType.PDF));
        when(externalFileType.getExternalFileTypeByExt("pdf")).thenReturn(Optional.of(StandardExternalFileType.PDF));
        tempFile = tempFolder.resolve("temporaryFile");
        Files.createFile(tempFile);
    }

    @Test
    void deleteWhenFilePathNotPresentReturnsTrue() {
        // Making this a spy, so we can inject an empty optional without digging into the implementation
        linkedFile = spy(new LinkedFile("", "nonexistent file", ""));
        doReturn(Optional.empty()).when(linkedFile).findIn(any(BibDatabaseContext.class), any(FilePreferences.class));

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences, externalFileType);
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

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences, externalFileType);
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

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences, externalFileType);
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

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences, externalFileType);
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

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences, externalFileType);
        boolean removed = viewModel.delete();

        assertFalse(removed);
        assertTrue(Files.exists(tempFile));
    }

    @Test
    void downloadDoesNotOverwriteFileTypeExtension() throws MalformedURLException {
        linkedFile = new LinkedFile(new URL("http://arxiv.org/pdf/1207.0408v1"), "");

        databaseContext = mock(BibDatabaseContext.class);
        when(filePreferences.getFileNamePattern()).thenReturn("[bibtexkey]");
        when(databaseContext.getFirstExistingFileDir(any())).thenReturn(Optional.of(tempFile.getParent()));
        when(databaseContext.getFileDirectoriesAsPaths(any())).thenReturn(Collections.singletonList(tempFile.getParent()));
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(linkedFileHandler.getSuggestedFileName("PDF")).thenReturn("asdf.pdf");
        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, new CurrentThreadTaskExecutor(), dialogService, preferences, externalFileType);

        BackgroundTask<Path> task = viewModel.prepareDownloadTask(tempFile.getParent(), new URLDownload("http://arxiv.org/pdf/1207.0408v1"));
        task.onSuccess(destination -> {
            LinkedFile newLinkedFile = LinkedFilesEditorViewModel.fromFile(destination, databaseContext.getFileDirectoriesAsPaths(filePreferences), externalFileType);

            assertEquals("PDF", newLinkedFile.getFileType());

        });
        new CurrentThreadTaskExecutor().execute(task);

        viewModel.download();

    }
}
