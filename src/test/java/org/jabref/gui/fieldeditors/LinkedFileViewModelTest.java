package org.jabref.gui.fieldeditors;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.StandardExternalFileType;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LinkedFileViewModelTest {

    private Path tempFile;
    private LinkedFile linkedFile;
    private BibEntry entry;
    private BibDatabaseContext databaseContext;
    private TaskExecutor taskExecutor;
    private DialogService dialogService;
    private final ExternalFileTypes externalFileType = mock(ExternalFileTypes.class);
    private final FilePreferences filePreferences = mock(FilePreferences.class);
    private final PreferencesService preferences = mock(PreferencesService.class);
    private CookieManager cookieManager;

    @BeforeEach
    void setUp(@TempDir Path tempFolder) throws Exception {
        entry = new BibEntry();
        entry.setCitationKey("asdf");
        databaseContext = new BibDatabaseContext();
        taskExecutor = mock(TaskExecutor.class);
        dialogService = mock(DialogService.class);

        when(externalFileType.getExternalFileTypeSelection()).thenReturn(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes()));
        when(externalFileType.getExternalFileTypeByMimeType("application/pdf")).thenReturn(Optional.of(StandardExternalFileType.PDF));
        when(externalFileType.getExternalFileTypeByMimeType(contains("text/html"))).thenReturn(Optional.of(StandardExternalFileType.URL));
        when(externalFileType.getExternalFileTypeByExt("pdf")).thenReturn(Optional.of(StandardExternalFileType.PDF));
        when(externalFileType.getExternalFileTypeByExt("html")).thenReturn(Optional.of(StandardExternalFileType.URL));
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(preferences.getXmpPreferences()).thenReturn(mock(XmpPreferences.class));
        tempFile = tempFolder.resolve("temporaryFile");
        Files.createFile(tempFile);

        // Check if there exists a system wide cookie handler
        if (CookieHandler.getDefault() == null) {
            cookieManager = new CookieManager();
            CookieHandler.setDefault(cookieManager);
        } else {
            cookieManager = (CookieManager) CookieHandler.getDefault();
        }
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
    }

    @AfterEach
    void tearDown() {
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_NONE);
    }

    @Test
    void deleteWhenFilePathNotPresentReturnsTrue() {
        // Making this a spy, so we can inject an empty optional without digging into the implementation
        linkedFile = spy(new LinkedFile("", Path.of("nonexistent file"), ""));
        doReturn(Optional.empty()).when(linkedFile).findIn(any(BibDatabaseContext.class), any(FilePreferences.class));

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences, externalFileType);
        boolean removed = viewModel.delete();

        assertTrue(removed);
        verifyNoInteractions(dialogService); // dialog was never shown
    }

    @Test
    void deleteWhenRemoveChosenReturnsTrueButDoesNotDeletesFile() {
        linkedFile = new LinkedFile("", tempFile, "");
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
        linkedFile = new LinkedFile("", tempFile, "");
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
        linkedFile = new LinkedFile("", Path.of("!!nonexistent file!!"), "");
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
        linkedFile = new LinkedFile("desc", tempFile, "pdf");
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
    void downloadHtmlFileCausesWarningDisplay() throws MalformedURLException {
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("[entrytype]");
        databaseContext.setDatabasePath(tempFile);

        URL url = new URL("https://www.google.com/");
        String fileType = StandardExternalFileType.URL.getName();
        linkedFile = new LinkedFile(url, fileType);

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, new CurrentThreadTaskExecutor(), dialogService, preferences, externalFileType);

        viewModel.download();

        verify(dialogService, atLeastOnce()).notify("Downloaded website as an HTML file.");
    }

    @Test
    void downloadDoesNotOverwriteFileTypeExtension() throws MalformedURLException {
        linkedFile = new LinkedFile(new URL("http://arxiv.org/pdf/1207.0408v1"), "");

        databaseContext = mock(BibDatabaseContext.class);
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("");

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, new CurrentThreadTaskExecutor(), dialogService, preferences, externalFileType);

        BackgroundTask<Path> task = viewModel.prepareDownloadTask(tempFile.getParent(), new URLDownload("http://arxiv.org/pdf/1207.0408v1"));
        task.onSuccess(destination -> {
            LinkedFile newLinkedFile = LinkedFilesEditorViewModel.fromFile(destination, Collections.singletonList(tempFile.getParent()), externalFileType);
            assertEquals("asdf.pdf", newLinkedFile.getLink());
            assertEquals("PDF", newLinkedFile.getFileType());
        });
        task.onFailure(Assertions::fail);
        new CurrentThreadTaskExecutor().execute(task);
    }

    @FetcherTest
    @Test
    void downloadHtmlWhenLinkedFilePointsToHtml() throws MalformedURLException {
        // the link mentioned in issue #7452
        String url = "https://onlinelibrary.wiley.com/doi/abs/10.1002/0470862106.ia615";
        String fileType = StandardExternalFileType.URL.getName();
        linkedFile = new LinkedFile(new URL(url), fileType);

        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("[entrytype]");

        databaseContext.setDatabasePath(tempFile);

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, new CurrentThreadTaskExecutor(), dialogService, preferences, externalFileType);

        viewModel.download();

        List<LinkedFile> linkedFiles = entry.getFiles();

        for (LinkedFile file: linkedFiles) {
            if (file.getLink().equalsIgnoreCase("Misc/asdf.html")) {
                assertEquals("URL", file.getFileType());
                return;
            }
        }
        // If the file was not found among the linked files to the entry
        fail();
    }

    @Test
    void isNotSamePath() {
        linkedFile = new LinkedFile("desc", tempFile, "pdf");
        databaseContext = mock(BibDatabaseContext.class);
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        when(databaseContext.getFirstExistingFileDir(filePreferences)).thenReturn(Optional.of(Path.of("/home")));

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences, externalFileType);
        assertFalse(viewModel.isGeneratedPathSameAsOriginal());
    }

    @Test
    void isSamePath() {
        linkedFile = new LinkedFile("desc", tempFile, "pdf");
        databaseContext = mock(BibDatabaseContext.class);
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        when(databaseContext.getFirstExistingFileDir(filePreferences)).thenReturn(Optional.of(tempFile.getParent()));

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences, externalFileType);
        assertTrue(viewModel.isGeneratedPathSameAsOriginal());
    }


    // Tests if added parameters to mimeType gets parsed to correct format.
    @Test
    void mimeTypeStringWithParameterIsReturnedAsWithoutParameter() {
        Optional<ExternalFileType> test = externalFileType.getExternalFileTypeByMimeType("text/html; charset=UTF-8");
        String actual = test.get().toString();
        assertEquals("URL", actual);
    }

    @Test
    void downloadPdfFileWhenLinkedFilePointsToPdfUrl() throws MalformedURLException {
        linkedFile = new LinkedFile(new URL("http://arxiv.org/pdf/1207.0408v1"), "pdf");
        // Needed Mockito stubbing methods to run test
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("[entrytype]");

        databaseContext.setDatabasePath(tempFile);

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, new CurrentThreadTaskExecutor(), dialogService, preferences, externalFileType);
        viewModel.download();

        // Loop through downloaded files to check for filetype='pdf'
        List<LinkedFile> linkedFiles = entry.getFiles();
        for (LinkedFile files : linkedFiles) {
            if (files.getLink().equalsIgnoreCase("Misc/asdf.pdf")) {
                assertEquals("pdf", files.getFileType().toLowerCase());
                return;
            }
        }
        // Assert fail if no PDF type was found
        fail();
    }

}
