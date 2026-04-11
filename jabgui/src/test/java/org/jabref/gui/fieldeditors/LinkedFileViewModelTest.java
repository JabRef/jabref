package org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import javafx.collections.FXCollections;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.externalfiletype.StandardExternalFileType;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.externalfiles.LinkedFileHandler;
import org.jabref.logic.pdf.PdfPageNumberParser;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.URLUtil;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Need to run on JavaFX thread since {@link org.jabref.gui.linkedfile.DeleteFileAction.execute} creates a DialogPane
@ExtendWith(ApplicationExtension.class)
class LinkedFileViewModelTest {

    private Path tempFile;
    private LinkedFile linkedFile;
    private BibEntry entry;
    private BibDatabaseContext databaseContext;
    private TaskExecutor taskExecutor;
    private DialogService dialogService;
    private final ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
    private final FilePreferences filePreferences = mock(FilePreferences.class);
    private final GuiPreferences preferences = mock(GuiPreferences.class);
    private CookieManager cookieManager;
    private HttpServer httpServer;
    private String htmlUrl;
    private String pdfUrl;

    @BeforeEach
    void setUp(@TempDir Path tempFolder) throws IOException {
        entry = new BibEntry()
                .withCitationKey("asdf");

        databaseContext = new BibDatabaseContext();
        taskExecutor = mock(TaskExecutor.class);
        dialogService = mock(DialogService.class);

        when(externalApplicationsPreferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));
        when(filePreferences.confirmDeleteLinkedFile()).thenReturn(true);
        when(preferences.getExternalApplicationsPreferences()).thenReturn(externalApplicationsPreferences);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(preferences.getXmpPreferences()).thenReturn(mock(XmpPreferences.class));
        tempFile = tempFolder.resolve("temporaryFile");
        Files.createFile(tempFile);

        // Check if there exists a system-wide cookie handler
        if (CookieHandler.getDefault() == null) {
            cookieManager = new CookieManager();
            CookieHandler.setDefault(cookieManager);
        } else {
            cookieManager = (CookieManager) CookieHandler.getDefault();
        }
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        startLocalHttpServer();
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_NONE);
    }

    private void startLocalHttpServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        byte[] htmlContent = "<html><body>test html</body></html>".getBytes(StandardCharsets.UTF_8);
        httpServer.createContext("/index.html", exchange -> sendResponse(exchange, "text/html; charset=UTF-8", htmlContent));

        byte[] pdfContent = """
                %PDF-1.4
                1 0 obj
                << /Type /Catalog >>
                endobj
                trailer
                << /Root 1 0 R >>
                %%EOF
                """.getBytes(StandardCharsets.US_ASCII);
        httpServer.createContext("/paper.pdf", exchange -> sendResponse(exchange, "application/pdf", pdfContent));

        httpServer.start();
        int port = httpServer.getAddress().getPort();
        htmlUrl = "http://127.0.0.1:" + port + "/index.html";
        pdfUrl = "http://127.0.0.1:" + port + "/paper.pdf";
    }

    private void sendResponse(HttpExchange exchange, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, body.length);
        try (var outputStream = exchange.getResponseBody()) {
            outputStream.write(body);
        } finally {
            exchange.close();
        }
    }

    @Test
    void deleteWhenFilePathNotPresentReturnsTrue() {
        // Making this a spy, so we can inject an empty optional without digging into the implementation
        linkedFile = spy(new LinkedFile("", Path.of("nonexistent file"), ""));
        doReturn(Optional.empty()).when(linkedFile).findIn(any(BibDatabaseContext.class), any(FilePreferences.class));

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);
        boolean removed = viewModel.delete();

        assertTrue(removed);

        // We show "Error accessing file '%0'.", thus the next condition does not hold
        // verifyNoInteractions(dialogService); // dialog was never shown
    }

    @Test
    void deleteWhenRemoveChosenReturnsTrueButDoesNotDeletesFile() {
        linkedFile = new LinkedFile("", tempFile, "");
        // Mock the dialog created at {@link org.jabref.gui.linkedfile.DeleteFileAction.execute}
        when(dialogService.showCustomDialogAndWait(
                anyString(),
                any(DialogPane.class),
                any(ButtonType.class),
                any(ButtonType.class),
                any(ButtonType.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(2))); // first vararg - remove button

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);
        boolean removed = viewModel.delete();

        assertTrue(removed);
        assertTrue(Files.exists(tempFile));
    }

    @Test
    void deleteWhenDeleteChosenReturnsTrueAndDeletesFile() {
        linkedFile = new LinkedFile("", tempFile, "");
        when(dialogService.showCustomDialogAndWait(
                anyString(),
                any(DialogPane.class),
                any(ButtonType.class),
                any(ButtonType.class),
                any(ButtonType.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(3))); // second vararg - delete button

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);
        boolean removed = viewModel.delete();

        assertTrue(removed);
        assertFalse(Files.exists(tempFile));
    }

    @Test
    void deleteMissingFileReturnsTrue() {
        linkedFile = new LinkedFile("", Path.of("!!nonexistent file!!"), "");
        when(dialogService.showCustomDialogAndWait(
                anyString(),
                any(DialogPane.class),
                any(ButtonType.class),
                any(ButtonType.class),
                any(ButtonType.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(3))); // second vararg - delete button

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);
        boolean removed = viewModel.delete();

        assertTrue(removed);
    }

    @Test
    void deleteWhenDialogCancelledReturnsFalseAndDoesNotRemoveFile() {
        linkedFile = new LinkedFile("desc", tempFile, "pdf");
        when(dialogService.showCustomDialogAndWait(
                anyString(),
                any(DialogPane.class),
                any(ButtonType.class),
                any(ButtonType.class),
                any(ButtonType.class))).thenAnswer(invocation -> Optional.of(invocation.getArgument(4))); // third vararg - cancel button

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);
        boolean removed = viewModel.delete();

        assertFalse(removed);
        assertTrue(Files.exists(tempFile));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void downloadHtmlFileCausesWarningDisplay(boolean keepHtmlLink) throws MalformedURLException {
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("[entrytype]");
        databaseContext.setDatabasePath(tempFile);

        URL url = URLUtil.create(htmlUrl);
        String fileType = StandardExternalFileType.URL.getName();
        linkedFile = new LinkedFile(url, fileType);

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, new CurrentThreadTaskExecutor(), dialogService, preferences);

        viewModel.download(keepHtmlLink);

        String warningText = keepHtmlLink
                ? "Download '%s' was a HTML file. Keeping URL.".formatted(htmlUrl)
                : "Download '%s' was a HTML file. Removed.".formatted(htmlUrl);
        verify(dialogService, atLeastOnce()).notify(warningText);
    }

    @Test
    void isNotSamePath() {
        linkedFile = new LinkedFile("desc", tempFile, "pdf");
        databaseContext = mock(BibDatabaseContext.class);
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("");
        when(databaseContext.getFirstExistingFileDir(filePreferences)).thenReturn(Optional.of(Path.of("/home")));

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);
        assertFalse(viewModel.isGeneratedPathSameAsOriginal());
    }

    @Test
    void isSamePath() {
        linkedFile = new LinkedFile("desc", tempFile, "pdf");
        databaseContext = mock(BibDatabaseContext.class);
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("");
        when(databaseContext.getFirstExistingFileDir(filePreferences)).thenReturn(Optional.of(tempFile.getParent()));

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);
        assertTrue(viewModel.isGeneratedPathSameAsOriginal());
    }

    // Tests if isGeneratedPathSameAsOriginal takes into consideration File directory pattern
    @Test
    void isNotSamePathWithPattern() {
        linkedFile = new LinkedFile("desc", tempFile, "pdf");
        databaseContext = mock(BibDatabaseContext.class);
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("[entrytype]");
        when(databaseContext.getFirstExistingFileDir(filePreferences)).thenReturn(Optional.of(tempFile.getParent()));

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);
        assertFalse(viewModel.isGeneratedPathSameAsOriginal());
    }

    // Tests if isGeneratedPathSameAsOriginal takes into consideration File directory pattern
    @Test
    void isSamePathWithPattern() throws IOException {
        linkedFile = new LinkedFile("desc", tempFile, "pdf");
        databaseContext = mock(BibDatabaseContext.class);
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("[entrytype]");
        when(databaseContext.getFirstExistingFileDir(filePreferences)).thenReturn(Optional.of(tempFile.getParent()));

        LinkedFileHandler fileHandler = new LinkedFileHandler(linkedFile, entry, databaseContext, filePreferences);
        fileHandler.moveToDefaultDirectory();

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);
        assertTrue(viewModel.isGeneratedPathSameAsOriginal());
    }

    // Tests if added parameters to mimeType gets parsed to correct format.
    @Test
    void mimeTypeStringWithParameterIsReturnedAsWithoutParameter() {
        Optional<ExternalFileType> test = ExternalFileTypes.getExternalFileTypeByMimeType("text/html; charset=UTF-8", externalApplicationsPreferences);
        String actual = test.get().toString();
        assertEquals("URL", actual);
    }

    // We cannot use "@FetcherTest" annotation, because a @FetcherTest does not fire up a GUI environment (which is needed for this test)
    // @FetcherTest
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void downloadPdfFileWhenLinkedFilePointsToPdfUrl(boolean keepHtml) throws MalformedURLException {
        linkedFile = new LinkedFile(URLUtil.create(pdfUrl), "pdf");
        // Needed Mockito stubbing methods to run test
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("[entrytype]");

        databaseContext.setDatabasePath(tempFile);

        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, new CurrentThreadTaskExecutor(), dialogService, preferences);

        // TODO: Rewrite using WireMock
        viewModel.download(keepHtml);

        // Loop through downloaded files to check for filetype='pdf'
        List<LinkedFile> linkedFiles = entry.getFiles();
        for (LinkedFile files : linkedFiles) {
            if ("Misc/asdf.pdf".equalsIgnoreCase(files.getLink())) {
                assertEquals("pdf", files.getFileType().toLowerCase());
                return;
            }
        }
        // Assert fail if no PDF type was found
        fail();
    }

    @Test
    void parseFirstPageNumberExtractsFirstNumber() {
        assertEquals(Optional.of(73), PdfPageNumberParser.parseFirstPageNumber("73--96"));
        assertEquals(Optional.of(5), PdfPageNumberParser.parseFirstPageNumber("S5-S8"));
        assertEquals(Optional.empty(), PdfPageNumberParser.parseFirstPageNumber("no-pages"));
    }

    @Test
    void openForPdfPassesParsedFirstPageNumber() {
        linkedFile = new LinkedFile("desc", Path.of("paper.pdf"), "pdf");
        entry.setField(StandardField.PAGES, "73--96");
        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        try (MockedStatic<NativeDesktop> nativeDesktop = mockStatic(NativeDesktop.class)) {
            nativeDesktop.when(() -> NativeDesktop.openExternalFileAnyFormat(any(BibDatabaseContext.class), any(ExternalApplicationsPreferences.class), any(FilePreferences.class), anyString(), any(), anyInt()))
                         .thenReturn(true);

            viewModel.open();

            nativeDesktop.verify(() -> NativeDesktop.openExternalFileAnyFormat(
                    eq(databaseContext),
                    eq(externalApplicationsPreferences),
                    eq(filePreferences),
                    eq(linkedFile.getLink()),
                    any(),
                    eq(73)));
        }
    }

    @Test
    void openForNonPdfPassesDefaultPageNumber() {
        linkedFile = new LinkedFile("desc", Path.of("notes.txt"), "txt");
        entry.setField(StandardField.PAGES, "73--96");
        LinkedFileViewModel viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogService, preferences);

        try (MockedStatic<NativeDesktop> nativeDesktop = mockStatic(NativeDesktop.class)) {
            nativeDesktop.when(() -> NativeDesktop.openExternalFileAnyFormat(any(BibDatabaseContext.class), any(ExternalApplicationsPreferences.class), any(FilePreferences.class), anyString(), any(), anyInt()))
                         .thenReturn(true);

            viewModel.open();

            nativeDesktop.verify(() -> NativeDesktop.openExternalFileAnyFormat(
                    eq(databaseContext),
                    eq(externalApplicationsPreferences),
                    eq(filePreferences),
                    eq(linkedFile.getLink()),
                    any(),
                    eq(1)));
        }
    }
}
