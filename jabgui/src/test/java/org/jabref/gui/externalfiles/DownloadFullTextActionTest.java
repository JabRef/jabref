package org.jabref.gui.externalfiles;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.gui.util.EntryLookupsInProgress;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.FetcherResult;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.IdFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fetcher.TrustLevel;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
@ExtendWith(JavaFxExtension.class)
class DownloadFullTextActionTest {

    private DialogService dialogService;
    private JabRefGuiStateManager stateManager;
    private GuiPreferences preferences;
    private UiTaskExecutor taskExecutor;
    private BibDatabaseContext databaseContext;
    private BibEntry entry;
    private FetcherResult fetcherResult;
    private HttpServer httpServer;

    @BeforeEach
    void setUp() throws MalformedURLException {
        dialogService = mock(DialogService.class);
        stateManager = new JabRefGuiStateManager();
        preferences = mock(GuiPreferences.class);
        taskExecutor = mock(UiTaskExecutor.class);
        when(taskExecutor.execute(any(BackgroundTask.class))).thenReturn(CompletableFuture.completedFuture(null));

        databaseContext = new BibDatabaseContext();
        stateManager.getOpenDatabases().add(databaseContext);
        stateManager.setActiveDatabase(databaseContext);

        entry = new BibEntry()
                .withField(StandardField.TITLE, "Original title")
                .withField(StandardField.DOI, "10.1000/original");
        databaseContext.getDatabase().insertEntry(entry);
        stateManager.setSelectedEntries(List.of(entry));

        fetcherResult = new FetcherResult(TrustLevel.PUBLISHER, URLUtil.create("https://example.org/test.pdf"), Map.of());
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void downloadsForUnchangedEntry() throws Exception {
        RecordingDownloadFullTextAction action = new RecordingDownloadFullTextAction(_ -> Optional.of(fetcherResult));

        BackgroundTask<?> task = captureTask(action);
        completeTask(task);

        assertEquals(List.of(entry), action.downloadedEntries);
        assertEquals(List.of(fetcherResult), action.downloadedResults);
    }

    @Test
    void skipsDownloadWhenEntryChangedAfterLookup() throws Exception {
        RecordingDownloadFullTextAction action = new RecordingDownloadFullTextAction(_ -> Optional.of(fetcherResult));

        BackgroundTask<?> task = captureTask(action);
        Object downloads = task.call();
        entry.withField(StandardField.TITLE, "Updated title");
        runSuccessHandler(task, downloads);

        assertEquals(List.of(), action.downloadedEntries);
    }

    @Test
    void skipsDownloadWhenEntryDeletedAfterLookup() throws Exception {
        RecordingDownloadFullTextAction action = new RecordingDownloadFullTextAction(_ -> Optional.of(fetcherResult));

        BackgroundTask<?> task = captureTask(action);
        Object downloads = task.call();
        databaseContext.getDatabase().removeEntry(entry);
        runSuccessHandler(task, downloads);

        assertEquals(List.of(), action.downloadedEntries);
    }

    @Test
    void finderReceivesEntrySnapshot() throws Exception {
        RecordingDownloadFullTextAction action = new RecordingDownloadFullTextAction(snapshot -> {
            assertNotSame(entry, snapshot);
            assertEquals(Optional.of("Original title"), snapshot.getField(StandardField.TITLE));
            return Optional.of(fetcherResult);
        });

        BackgroundTask<?> task = captureTask(action);
        completeTask(task);

        assertEquals(List.of(entry), action.downloadedEntries);
    }

    @Test
    void notifiesWhenNoFullTextDocumentIsFound() throws Exception {
        RecordingDownloadFullTextAction action =
                new RecordingDownloadFullTextAction(_ -> Optional.empty());

        BackgroundTask<?> task = captureTask(action);
        completeTask(task);

        assertEquals(List.of(), action.downloadedEntries);
        assertEquals(List.of(), entry.getFiles());
        verify(dialogService).notify(ArgumentMatchers.anyString());
    }

    @Test
    void downloadsAndAttachesFullTextForEntryWithDoi(@TempDir Path tempDir) throws Exception {
        // Arrange: create a small PDF response for our local HTTP server.
        byte[] pdfContent = "%PDF-1.4\nScenario B test PDF\n%%EOF"
                .getBytes(StandardCharsets.US_ASCII);

        httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        AtomicInteger finderCalls = new AtomicInteger();

        httpServer.createContext("/test.pdf", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/pdf");

            if ("HEAD".equals(exchange.getRequestMethod())) {
                // The download action may check whether the URL is reachable first.
                exchange.sendResponseHeaders(200, -1);
            } else if ("GET".equals(exchange.getRequestMethod())) {
                // Send the actual PDF content.
                exchange.sendResponseHeaders(200, pdfContent.length);
                exchange.getResponseBody().write(pdfContent);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }

            exchange.close();
        });

        httpServer.start();

        String pdfUrl = "http://localhost:"
                + httpServer.getAddress().getPort()
                + "/test.pdf";

        FetcherResult localFetcherResult = new FetcherResult(
                TrustLevel.PUBLISHER,
                URLUtil.create(pdfUrl),
                Map.of()
        );

        // Arrange: configure the file preferences and temporary download directory.
        FilePreferences filePreferences = mock(FilePreferences.class);
        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("");
        when(filePreferences.shouldKeepDownloadUrl()).thenReturn(true);
        when(externalApplicationsPreferences.getExternalFileTypes())
                .thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));

        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(preferences.getExternalApplicationsPreferences())
                .thenReturn(externalApplicationsPreferences);

        // Make the state manager use the spy instead of the original database context.
        BibDatabaseContext originalDatabaseContext = databaseContext;
        databaseContext = spy(originalDatabaseContext);

        stateManager.getOpenDatabases().remove(originalDatabaseContext);
        stateManager.getOpenDatabases().add(databaseContext);
        stateManager.setActiveDatabase(databaseContext);

        // Configure the temporary download directory.
        doReturn(List.of(tempDir))
                .when(databaseContext)
                .getFileDirectories(filePreferences);

        doReturn(Optional.of(tempDir))
                .when(databaseContext)
                .getFirstExistingFileDir(filePreferences);

        // Arrange: capture every background task submitted by the real workflow.
        List<BackgroundTask<?>> submittedTasks = new ArrayList<>();

        doAnswer(invocation -> {
            BackgroundTask<?> submittedTask = invocation.getArgument(0);
            submittedTasks.add(submittedTask);
            return CompletableFuture.completedFuture(null);
        }).when(taskExecutor).execute(any(BackgroundTask.class));

        // Arrange: use the real action, with a finder that returns our local PDF URL.
        DownloadFullTextAction action = new DownloadFullTextAction(
                dialogService,
                stateManager,
                preferences,
                taskExecutor,
                snapshot -> {
                    finderCalls.incrementAndGet();

                    assertNotSame(entry, snapshot);
                    assertEquals(
                            Optional.of("10.1000/original"),
                            snapshot.getField(StandardField.DOI)
                    );

                    return Optional.of(localFetcherResult);
                }
        );

        // Act: execute the full-text lookup.
        action.execute();

        assertEquals(1, submittedTasks.size());

        // Execute the lookup task.
        BackgroundTask<?> lookupTask = submittedTasks.getFirst();
        Object downloads = lookupTask.call();

        assertEquals(1, finderCalls.get());

        // Run the success handler.
        runSuccessHandler(lookupTask, downloads);

        // The successful lookup should have triggered a second task for downloading.
        assertEquals(2, submittedTasks.size());

        // Execute the actual file-download task and its success callback.
        BackgroundTask<?> downloadTask = submittedTasks.get(1);
        Object downloadResult = downloadTask.call();
        runSuccessHandler(downloadTask, downloadResult);

        // Assert: the original entry now has a linked file.
        assertEquals(1, entry.getFiles().size());

        LinkedFile downloadedFile = entry.getFiles().getFirst();

        // Assert: the linked file points to a real local file.
        Path downloadedPath = tempDir.resolve(downloadedFile.getLink());

        assertTrue(Files.exists(downloadedPath));
        assertArrayEquals(pdfContent, Files.readAllBytes(downloadedPath));

        // Assert: the original DOI has not been changed.
        assertEquals(
                Optional.of("10.1000/original"),
                entry.getField(StandardField.DOI)
        );
    }

    @Test
    void downloadsAndAttachesFullTextWhenExistingFileIsMissing(@TempDir Path tempDir) throws Exception {
        // Arrange: create a small PDF response for our local HTTP server.
        byte[] pdfContent = "%PDF-1.4\nScenario F test PDF\n%%EOF"
                .getBytes(StandardCharsets.US_ASCII);

        httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        httpServer.createContext("/test.pdf", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/pdf");

            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
            } else if ("GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, pdfContent.length);
                exchange.getResponseBody().write(pdfContent);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }

            exchange.close();
        });

        httpServer.start();

        String pdfUrl = "http://localhost:"
                + httpServer.getAddress().getPort()
                + "/test.pdf";

        FetcherResult localFetcherResult = new FetcherResult(
                TrustLevel.PUBLISHER,
                URLUtil.create(pdfUrl),
                Map.of()
        );

        // Arrange: configure the file preferences and temporary download directory.
        FilePreferences filePreferences = mock(FilePreferences.class);
        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("");
        when(filePreferences.shouldKeepDownloadUrl()).thenReturn(true);
        when(externalApplicationsPreferences.getExternalFileTypes())
                .thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));

        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(preferences.getExternalApplicationsPreferences())
                .thenReturn(externalApplicationsPreferences);

        // Make the state manager use the spy instead of the original database context.
        BibDatabaseContext originalDatabaseContext = databaseContext;
        databaseContext = spy(originalDatabaseContext);

        stateManager.getOpenDatabases().remove(originalDatabaseContext);
        stateManager.getOpenDatabases().add(databaseContext);
        stateManager.setActiveDatabase(databaseContext);

        // Configure the temporary download directory.
        doReturn(List.of(tempDir))
                .when(databaseContext)
                .getFileDirectories(filePreferences);

        doReturn(Optional.of(tempDir))
                .when(databaseContext)
                .getFirstExistingFileDir(filePreferences);

        // Arrange: create an existing file link pointing to a missing PDF.
        LinkedFile missingFile = new LinkedFile(
                "",
                Path.of("missing.pdf"),
                "PDF"
        );
        entry.setFiles(List.of(missingFile));

        // Arrange: capture every background task submitted by the real workflow.
        List<BackgroundTask<?>> submittedTasks = new ArrayList<>();

        doAnswer(invocation -> {
            BackgroundTask<?> submittedTask = invocation.getArgument(0);
            submittedTasks.add(submittedTask);
            return CompletableFuture.completedFuture(null);
        }).when(taskExecutor).execute(any(BackgroundTask.class));

        // Arrange: use the real action, with a finder that returns our local PDF URL.
        DownloadFullTextAction action = new DownloadFullTextAction(
                dialogService,
                stateManager,
                preferences,
                taskExecutor,
                snapshot -> {
                    assertNotSame(entry, snapshot);
                    assertEquals(
                            Optional.of("10.1000/original"),
                            snapshot.getField(StandardField.DOI)
                    );

                    return Optional.of(localFetcherResult);
                }
        );

        // Act: execute the full-text lookup.
        action.execute();

        assertEquals(1, submittedTasks.size());

        // Execute the lookup task.
        BackgroundTask<?> lookupTask = submittedTasks.getFirst();
        Object downloads = lookupTask.call();

        // Run the success handler.
        runSuccessHandler(lookupTask, downloads);

        // The successful lookup should have triggered a second task for downloading.
        assertEquals(2, submittedTasks.size());

        // Execute the actual file-download task and its success callback.
        BackgroundTask<?> downloadTask = submittedTasks.get(1);
        Object downloadResult = downloadTask.call();
        runSuccessHandler(downloadTask, downloadResult);

        // Assert: the original broken link remains and the downloaded file is added.
        assertEquals(2, entry.getFiles().size());
        assertTrue(entry.getFiles().contains(missingFile));

        LinkedFile downloadedFile = entry.getFiles().stream()
                                         .filter(file -> !file.equals(missingFile))
                                         .findFirst()
                                         .orElseThrow();

        // Assert: the newly downloaded file exists and contains the expected content.
        Path downloadedPath = tempDir.resolve(downloadedFile.getLink());

        assertTrue(Files.exists(downloadedPath));
        assertArrayEquals(pdfContent, Files.readAllBytes(downloadedPath));

        // Assert: the original DOI has not been changed.
        assertEquals(
                Optional.of("10.1000/original"),
                entry.getField(StandardField.DOI)
        );
    }

    @Test
    void downloadsAndAttachesFullTextWhenExistingFileIsMissingAndDoiIsDiscoveredFromTitle(
            @TempDir Path tempDir) throws Exception {

        // Arrange: create a small PDF response for our local HTTP server.
        byte[] pdfContent = "%PDF-1.4\nScenario G test PDF\n%%EOF"
                .getBytes(StandardCharsets.US_ASCII);

        httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        httpServer.createContext("/test.pdf", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/pdf");

            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
            } else if ("GET".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, pdfContent.length);
                exchange.getResponseBody().write(pdfContent);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }

            exchange.close();
        });

        httpServer.start();

        String pdfUrl = "http://localhost:"
                + httpServer.getAddress().getPort()
                + "/test.pdf";

        // Arrange: configure file preferences and temporary download directory.
        FilePreferences filePreferences = mock(FilePreferences.class);
        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        when(filePreferences.getFileNamePattern()).thenReturn("[citationkey]");
        when(filePreferences.getFileDirectoryPattern()).thenReturn("");
        when(filePreferences.shouldKeepDownloadUrl()).thenReturn(true);
        when(externalApplicationsPreferences.getExternalFileTypes())
                .thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));

        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(preferences.getExternalApplicationsPreferences())
                .thenReturn(externalApplicationsPreferences);

        // Make the state manager use the spy instead of the original database context.
        BibDatabaseContext originalDatabaseContext = databaseContext;
        databaseContext = spy(originalDatabaseContext);

        stateManager.getOpenDatabases().remove(originalDatabaseContext);
        stateManager.getOpenDatabases().add(databaseContext);
        stateManager.setActiveDatabase(databaseContext);

        // Configure the temporary download directory.
        doReturn(List.of(tempDir))
                .when(databaseContext)
                .getFileDirectories(filePreferences);

        doReturn(Optional.of(tempDir))
                .when(databaseContext)
                .getFirstExistingFileDir(filePreferences);

        // Arrange: create an entry with a title but no DOI.
        entry.setField(
                StandardField.TITLE,
                "A Test Article About Computer Science"
        );

        LinkedFile missingFile = new LinkedFile(
                "",
                Path.of("missing.pdf"),
                "PDF"
        );

        entry.setFiles(List.of(missingFile));

        entry.clearField(StandardField.DOI);

        assertTrue(entry.getField(StandardField.DOI).isEmpty());

        // Arrange: mock DOI discovery.
        DOI discoveredDoi = new DOI("10.1000/discovered");

        IdFetcher<DOI> doiFetcher = mock(IdFetcher.class);

        when(doiFetcher.findIdentifier(any(BibEntry.class)))
                .thenReturn(Optional.of(discoveredDoi));

        // Arrange: mock full-text retrieval.
        FulltextFetcher fulltextFetcher = mock(FulltextFetcher.class);

        when(fulltextFetcher.getTrustLevel())
                .thenReturn(TrustLevel.PUBLISHER);

        when(fulltextFetcher.findFullText(any(BibEntry.class)))
                .thenReturn(Optional.of(URLUtil.create(pdfUrl)));

        // Arrange: capture background tasks submitted by the real workflow.
        List<BackgroundTask<?>> submittedTasks = new ArrayList<>();

        doAnswer(invocation -> {
            BackgroundTask<?> submittedTask = invocation.getArgument(0);
            submittedTasks.add(submittedTask);
            return CompletableFuture.completedFuture(null);
        }).when(taskExecutor).execute(any(BackgroundTask.class));

        // Arrange: use the real DownloadFullTextAction constructor.
        DownloadFullTextAction action = new DownloadFullTextAction(
                dialogService,
                stateManager,
                preferences,
                taskExecutor
        );

        // Act: run DOI discovery and full-text lookup.
        try (MockedStatic<WebFetchers> webFetchers = mockStatic(WebFetchers.class)) {
            webFetchers.when(() -> WebFetchers.getIdFetcherForIdentifier(
                               eq(DOI.class),
                               nullable(ImporterPreferences.class)))
                       .thenReturn(doiFetcher);

            webFetchers.when(() -> WebFetchers.getFullTextFetchers(
                               nullable(ImportFormatPreferences.class),
                               nullable(ImporterPreferences.class)))
                       .thenReturn(Set.of(fulltextFetcher));

            action.execute();

            // The first task should be the full-text lookup.
            assertEquals(1, submittedTasks.size());

            BackgroundTask<?> lookupTask = submittedTasks.getFirst();
            Object downloads = lookupTask.call();

            runSuccessHandler(lookupTask, downloads);

            // Verify that DOI discovery and full-text retrieval occurred.
            verify(doiFetcher).findIdentifier(any(BibEntry.class));

            verify(fulltextFetcher).findFullText(argThat(discoveredEntry ->
                    discoveredEntry.getField(StandardField.DOI)
                                   .equals(Optional.of("10.1000/discovered"))
            ));
        }

        // The successful lookup should have triggered a second task for downloading.
        assertEquals(2, submittedTasks.size());

        // Execute the actual file-download task and its success callback.
        BackgroundTask<?> downloadTask = submittedTasks.get(1);
        Object downloadResult = downloadTask.call();

        runSuccessHandler(downloadTask, downloadResult);

        // Assert: the original broken link remains and the downloaded file is added.
        assertEquals(2, entry.getFiles().size());
        assertTrue(entry.getFiles().contains(missingFile));

        LinkedFile downloadedFile = entry.getFiles().stream()
                                         .filter(file -> !file.equals(missingFile))
                                         .findFirst()
                                         .orElseThrow();

        // Assert: the downloaded file exists and contains the expected content.
        Path downloadedPath = tempDir.resolve(downloadedFile.getLink());

        assertTrue(Files.exists(downloadedPath));
        assertArrayEquals(pdfContent, Files.readAllBytes(downloadedPath));

        // Assert: DOI discovery did not modify the original database entry.
        assertTrue(entry.getField(StandardField.DOI).isEmpty());
    }

    @Test
    void doesNotDownloadFullTextWhenDoiCannotBeDiscoveredFromTitle() throws Exception {
        // Arrange: configure the entry with a title but no DOI.
        entry.setField(
                StandardField.TITLE,
                "A Test Article About Computer Science"
        );

        LinkedFile missingFile = new LinkedFile(
                "",
                Path.of("missing.pdf"),
                "PDF"
        );

        entry.setFiles(List.of(missingFile));
        entry.clearField(StandardField.DOI);

        assertTrue(entry.getField(StandardField.DOI).isEmpty());

        // Arrange: mock DOI discovery to return no DOI.
        IdFetcher<DOI> doiFetcher = mock(IdFetcher.class);

        when(doiFetcher.findIdentifier(any(BibEntry.class)))
                .thenReturn(Optional.empty());

        // Arrange: mock full-text retrieval to return no PDF.
        FulltextFetcher fulltextFetcher = mock(FulltextFetcher.class);

        when(fulltextFetcher.getTrustLevel())
                .thenReturn(TrustLevel.PUBLISHER);

        when(fulltextFetcher.findFullText(any(BibEntry.class)))
                .thenReturn(Optional.empty());

        // Arrange: capture background tasks submitted by the workflow.
        List<BackgroundTask<?>> submittedTasks = new ArrayList<>();

        doAnswer(invocation -> {
            BackgroundTask<?> submittedTask = invocation.getArgument(0);
            submittedTasks.add(submittedTask);
            return CompletableFuture.completedFuture(null);
        }).when(taskExecutor).execute(any(BackgroundTask.class));

        DownloadFullTextAction action = new DownloadFullTextAction(
                dialogService,
                stateManager,
                preferences,
                taskExecutor
        );

        // Act: run DOI discovery and full-text lookup.
        try (MockedStatic<WebFetchers> webFetchers = mockStatic(WebFetchers.class)) {
            webFetchers.when(() -> WebFetchers.getIdFetcherForIdentifier(
                               eq(DOI.class),
                               nullable(ImporterPreferences.class)))
                       .thenReturn(doiFetcher);

            webFetchers.when(() -> WebFetchers.getFullTextFetchers(
                               nullable(ImportFormatPreferences.class),
                               nullable(ImporterPreferences.class)))
                       .thenReturn(Set.of(fulltextFetcher));

            action.execute();

            assertEquals(1, submittedTasks.size());

            BackgroundTask<?> lookupTask = submittedTasks.getFirst();
            Object downloads = lookupTask.call();

            runSuccessHandler(lookupTask, downloads);

            // Verify that DOI discovery and full-text retrieval were attempted.
            verify(doiFetcher).findIdentifier(any(BibEntry.class));
            verify(fulltextFetcher).findFullText(any(BibEntry.class));
        }

        // Assert: no download task was submitted.
        assertEquals(1, submittedTasks.size());

        // Assert: the original entry still has no DOI.
        assertTrue(entry.getField(StandardField.DOI).isEmpty());

        // Assert: the user was notified that no full-text document was found.
        verify(dialogService).notify(ArgumentMatchers.anyString());
    }

    @Test
    void queuedOrFinishedTaskLeavesNoFulltextLookupInProgress() throws Exception {
        RecordingDownloadFullTextAction action = new RecordingDownloadFullTextAction(_ -> Optional.empty());

        BackgroundTask<?> task = captureTask(action);
        assertFalse(EntryLookupsInProgress.FULLTEXT.inProgress(entry).get());

        task.call();
        drainJavaFxEvents();
        assertFalse(EntryLookupsInProgress.FULLTEXT.inProgress(entry).get());
    }

    @Test
    void entryShowsFulltextLookupInProgressWhileItsLookupRuns() throws Exception {
        List<Boolean> inProgressDuringLookup = new ArrayList<>();
        RecordingDownloadFullTextAction action = new RecordingDownloadFullTextAction(_ -> {
            inProgressDuringLookup.add(UiTaskExecutor.runInJavaFXThread(() -> EntryLookupsInProgress.FULLTEXT.inProgress(entry).get()));
            return Optional.empty();
        });

        captureTask(action).call();

        assertEquals(List.of(true), inProgressDuringLookup);
    }

    private static void drainJavaFxEvents() throws InterruptedException {
        CountDownLatch drained = new CountDownLatch(1);
        Platform.runLater(drained::countDown);
        assertTrue(drained.await(5, TimeUnit.SECONDS));
    }

    private BackgroundTask<?> captureTask(DownloadFullTextAction action) {
        action.execute();

        ArgumentCaptor<BackgroundTask> taskCaptor = ArgumentCaptor.forClass(BackgroundTask.class);
        verify(taskExecutor).execute(taskCaptor.capture());
        return taskCaptor.getValue();
    }

    private static void completeTask(BackgroundTask<?> task) throws Exception {
        Object downloads = task.call();
        runSuccessHandler(task, downloads);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void runSuccessHandler(BackgroundTask<?> task, Object downloads) {
        Consumer onSuccess = task.getOnSuccess();
        if (onSuccess != null) {
            onSuccess.accept(downloads);
        }
    }

    private class RecordingDownloadFullTextAction extends DownloadFullTextAction {
        private final List<BibEntry> downloadedEntries = new ArrayList<>();
        private final List<FetcherResult> downloadedResults = new ArrayList<>();

        RecordingDownloadFullTextAction(Function<BibEntry, Optional<FetcherResult>> fullTextFinder) {
            super(dialogService, stateManager, preferences, taskExecutor, fullTextFinder);
        }

        @Override
        void addLinkedFileFromURL(BibDatabaseContext databaseContext, FetcherResult result, BibEntry entry) {
            downloadedEntries.add(entry);
            downloadedResults.add(result);
        }
    }
}
