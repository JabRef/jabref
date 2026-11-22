package org.jabref.gui.importer;

import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.undo.JabRefUndoManager;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Answers;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class ImportEntriesViewModelTest {
    @ParameterizedTest
    @CsvSource({"true, true", "true, false", "false, true", "false, false"})
    void importRemembersDialogChoiceAndPassesItToHandlerWithoutChangingWebSearch(boolean choice, boolean webSearchChoice) {
        // [utest->req~import.dialog.download-linked-files~1]
        FilePreferences files = FilePreferences.getDefault();
        files.setDownloadLinkedFiles(webSearchChoice);
        files.setImportDialogDownloadLinkedFiles(!choice);
        GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getFilePreferences()).thenReturn(files);
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        BibDatabaseContext database = new BibDatabaseContext();
        List<BibEntry> entries = List.of(new BibEntry());
        ImportEntriesViewModel viewModel = new ImportEntriesViewModel(
                BackgroundTask.wrap(() -> new ParserResult(entries)),
                new CurrentThreadTaskExecutor(), database, mock(DialogService.class),
                new JabRefUndoManager(), preferences, mock(StateManager.class),
                new BibEntryTypesManager(), new DummyFileUpdateMonitor(), Optional.empty(), Optional.empty());
        viewModel.selectedDbProperty().set(database);
        assertEquals(!choice, viewModel.shouldDownloadLinkedFiles());

        try (MockedConstruction<ImportHandler> handlers = mockConstruction(ImportHandler.class)) {
            viewModel.importEntries(entries, choice);

            assertEquals(1, handlers.constructed().size());
            ImportHandler importHandler = handlers.constructed().getFirst();

            if (choice) {
                verify(importHandler).enableLinkedFileDownloads();
                verify(importHandler, never()).disableLinkedFileDownloads();
            } else {
                verify(importHandler).disableLinkedFileDownloads();
                verify(importHandler, never()).enableLinkedFileDownloads();
            }

            verify(importHandler).importEntriesWithDuplicateCheck(isNull(), eq(entries), any());
        }
        assertEquals(choice, files.shouldImportDialogDownloadLinkedFiles());
        assertEquals(webSearchChoice, files.shouldDownloadLinkedFiles());
    }

    @ParameterizedTest
    @CsvSource({"true, true", "true, false", "false, true", "false, false"})
    void webSearchUsesItsOwnPreferenceWithoutChangingImportChoice(boolean choice, boolean importChoice) {
        // [utest->req~import.dialog.download-linked-files~1]
        FilePreferences files = FilePreferences.getDefault();
        files.setDownloadLinkedFiles(!choice);
        files.setImportDialogDownloadLinkedFiles(importChoice);
        GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getFilePreferences()).thenReturn(files);
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        BibDatabaseContext database = new BibDatabaseContext();
        List<BibEntry> entries = List.of(new BibEntry());
        ImportEntriesViewModel viewModel = new ImportEntriesViewModel(
                BackgroundTask.wrap(() -> new ParserResult(entries)),
                new CurrentThreadTaskExecutor(), database, mock(DialogService.class),
                new JabRefUndoManager(), preferences, mock(StateManager.class),
                new BibEntryTypesManager(), new DummyFileUpdateMonitor(), Optional.empty(), Optional.empty());
        viewModel.selectedDbProperty().set(database);

        // DOI searches have no SearchBasedFetcher, so the source must be selected explicitly.
        viewModel.useWebSearchDownloadPreference();
        assertEquals(!choice, viewModel.shouldDownloadLinkedFiles());
        assertEquals(importChoice, files.shouldImportDialogDownloadLinkedFiles());
        assertEquals(!choice, files.shouldDownloadLinkedFiles());

        try (MockedConstruction<ImportHandler> handlers = mockConstruction(ImportHandler.class)) {
            viewModel.importEntries(entries, choice);

            assertEquals(1, handlers.constructed().size());
            ImportHandler importHandler = handlers.constructed().getFirst();
            if (choice) {
                verify(importHandler).enableLinkedFileDownloads();
                verify(importHandler, never()).disableLinkedFileDownloads();
            } else {
                verify(importHandler).disableLinkedFileDownloads();
                verify(importHandler, never()).enableLinkedFileDownloads();
            }
            verify(importHandler).importEntriesWithDuplicateCheck(isNull(), eq(entries), any());
        }
        assertEquals(choice, files.shouldDownloadLinkedFiles());
        assertEquals(importChoice, files.shouldImportDialogDownloadLinkedFiles());
    }
}
