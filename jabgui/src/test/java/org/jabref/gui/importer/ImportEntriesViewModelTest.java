package org.jabref.gui.importer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.AbbreviationPreferences;
import org.jabref.logic.journals.AbbreviationType;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.journals.JournalMode;
import org.jabref.logic.undo.JabRefUndoManager;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.paging.Page;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

    private AbbreviationPreferences abbreviationPreferences;
    private BibDatabaseContext databaseContext;
    private DialogService dialogService;
    private BibEntryTypesManager entryTypesManager;
    private FileUpdateMonitor fileUpdateMonitor;
    private GuiPreferences guiPreferences;
    private StateManager stateManager;
    private CurrentThreadTaskExecutor taskExecutor;
    private UndoManager undoManager;

    @BeforeEach
    void setUp() {
        Abbreviation abbreviation = new Abbreviation("Journal of Foo", "J. Foo");
        JournalAbbreviationRepository journalAbbreviationRepository = mock(JournalAbbreviationRepository.class);
        when(journalAbbreviationRepository.get("Journal of Foo")).thenReturn(Optional.of(abbreviation));
        when(journalAbbreviationRepository.get("J. Foo")).thenReturn(Optional.of(abbreviation));
        when(journalAbbreviationRepository.isKnownName("J. Foo")).thenReturn(true);
        when(journalAbbreviationRepository.isAbbreviatedName("J. Foo")).thenReturn(true);
        Injector.setModelOrService(JournalAbbreviationRepository.class, journalAbbreviationRepository);

        guiPreferences = mock(GuiPreferences.class);
        abbreviationPreferences = new AbbreviationPreferences(List.of(), false, false, JournalMode.NONE, AbbreviationType.DEFAULT);
        abbreviationPreferences.setAbbreviationTypeOnFetch(AbbreviationType.DEFAULT);
        when(guiPreferences.getAbbreviationPreferences()).thenReturn(abbreviationPreferences);
        when(guiPreferences.getFilePreferences()).thenReturn(FilePreferences.getDefault());

        databaseContext = new BibDatabaseContext();
        taskExecutor = new CurrentThreadTaskExecutor();
        dialogService = mock(DialogService.class);
        undoManager = mock(UndoManager.class);
        stateManager = mock(StateManager.class);
        entryTypesManager = mock(BibEntryTypesManager.class);
        fileUpdateMonitor = mock(FileUpdateMonitor.class);
    }

    @ParameterizedTest
    @CsvSource({
            "ABBREVIATE, Journal of Foo, J. Foo",
            "ABBREVIATE, J. Foo, J. Foo",
            "UNABBREVIATE, J. Foo, Journal of Foo",
            "UNABBREVIATE, Journal of Foo, Journal of Foo",
            "NONE, Journal of Foo, Journal of Foo",
            "NONE, J. Foo, J. Foo",
    })
    void initialFetchedEntriesFollowJournalPreference(JournalMode mode, String input, String expected) {
        abbreviationPreferences.setJournalAbbreviationMode(mode);

        BibEntry fetchedEntry = new BibEntry().withField(StandardField.JOURNAL, input);

        BackgroundTask<ParserResult> task = BackgroundTask.wrap(() -> new ParserResult(List.of(fetchedEntry)));

        ImportEntriesViewModel viewModel = new ImportEntriesViewModel(
                task,
                taskExecutor,
                databaseContext,
                dialogService,
                undoManager,
                guiPreferences,
                stateManager,
                entryTypesManager,
                fileUpdateMonitor,
                Optional.of(mock(SearchBasedFetcher.class)),
                Optional.of("test query"));

        ObservableList<BibEntry> entries = viewModel.getEntries();
        assertEquals(1, entries.size());
        assertEquals(Optional.of(expected), entries.getFirst().getField(StandardField.JOURNAL));
    }

    @ParameterizedTest
    @CsvSource({
            "ABBREVIATE, Journal of Foo, J. Foo",
            "ABBREVIATE, J. Foo, J. Foo",
            "UNABBREVIATE, J. Foo, Journal of Foo",
            "UNABBREVIATE, Journal of Foo, Journal of Foo",
            "NONE, Journal of Foo, Journal of Foo",
            "NONE, J. Foo, J. Foo"
    })
    void fetchMoreEntriesFollowsJournalPreference(JournalMode mode, String input, String expected) throws FetcherException {
        abbreviationPreferences.setJournalAbbreviationMode(mode);

        List<BibEntry> initialEntries = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            initialEntries.add(new BibEntry().withField(StandardField.TITLE, "Initial entry " + i));
        }
        BackgroundTask<ParserResult> task = BackgroundTask.wrap(() -> new ParserResult(initialEntries));
        BibEntry fetchedEntry = new BibEntry().withField(StandardField.JOURNAL, input);
        PagedSearchBasedFetcher fetcher = mock(PagedSearchBasedFetcher.class);
        when(fetcher.performSearchPaged("test query", 1))
                .thenReturn(new Page<>("test query", 1, List.of(fetchedEntry)));

        ImportEntriesViewModel viewModel = new ImportEntriesViewModel(
                task,
                taskExecutor,
                databaseContext,
                dialogService,
                undoManager,
                guiPreferences,
                stateManager,
                entryTypesManager,
                fileUpdateMonitor,
                Optional.of(fetcher),
                Optional.of("test query"));

        viewModel.fetchMoreEntries();

        verify(fetcher).performSearchPaged("test query", 1);
        assertEquals(21, viewModel.getAllEntries().size());
        ObservableList<BibEntry> entries = viewModel.getEntries();
        assertEquals(List.of(fetchedEntry), entries);
        assertEquals(Optional.of(expected), entries.getFirst().getField(StandardField.JOURNAL));
    }

    @ParameterizedTest
    @CsvSource({
            "ABBREVIATE, Journal of Foo",
            "UNABBREVIATE, J. Foo"
    })
    void initialNonFetchedEntriesKeepJournalDespiteConversionPreference(JournalMode mode, String journal) {
        abbreviationPreferences.setJournalAbbreviationMode(mode);

        BibEntry importedEntry = new BibEntry().withField(StandardField.JOURNAL, journal);
        BackgroundTask<ParserResult> task = BackgroundTask.wrap(() -> new ParserResult(List.of(importedEntry)));

        ImportEntriesViewModel viewModel = new ImportEntriesViewModel(
                task,
                taskExecutor,
                databaseContext,
                dialogService,
                undoManager,
                guiPreferences,
                stateManager,
                entryTypesManager,
                fileUpdateMonitor,
                Optional.empty(),
                Optional.empty());

        ObservableList<BibEntry> entries = viewModel.getEntries();
        assertEquals(1, entries.size());
        assertEquals(Optional.of(journal), entries.getFirst().getField(StandardField.JOURNAL));
    }

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

    @AfterEach
    void tearDown() {
        Injector.forgetAll();
    }
}

