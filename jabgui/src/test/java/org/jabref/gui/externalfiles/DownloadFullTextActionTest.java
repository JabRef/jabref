package org.jabref.gui.externalfiles;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.importer.FetcherResult;
import org.jabref.logic.importer.fetcher.TrustLevel;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
class DownloadFullTextActionTest {

    private DialogService dialogService;
    private JabRefGuiStateManager stateManager;
    private GuiPreferences preferences;
    private UiTaskExecutor taskExecutor;
    private BibDatabaseContext databaseContext;
    private BibEntry entry;
    private FetcherResult fetcherResult;

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

    @Test
    void downloadsForUnchangedEntry() throws Exception {
        RecordingDownloadFullTextAction action = new RecordingDownloadFullTextAction(snapshot -> Optional.of(fetcherResult));

        BackgroundTask<?> task = captureTask(action);
        completeTask(task);

        assertEquals(List.of(entry), action.downloadedEntries);
        assertEquals(List.of(fetcherResult), action.downloadedResults);
    }

    @Test
    void skipsDownloadWhenEntryChangedAfterLookup() throws Exception {
        RecordingDownloadFullTextAction action = new RecordingDownloadFullTextAction(snapshot -> Optional.of(fetcherResult));

        BackgroundTask<?> task = captureTask(action);
        Object downloads = task.call();
        entry.withField(StandardField.TITLE, "Updated title");
        runSuccessHandler(task, downloads);

        assertEquals(List.of(), action.downloadedEntries);
    }

    @Test
    void skipsDownloadWhenEntryDeletedAfterLookup() throws Exception {
        RecordingDownloadFullTextAction action = new RecordingDownloadFullTextAction(snapshot -> Optional.of(fetcherResult));

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
