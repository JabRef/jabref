package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.mergeentries.MergeDialogPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.gui.undo.HeadlessGuiUndoManager;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(JavaFxExtension.class)
class CitationsRelationsTabViewModelTest {
    private BibDatabaseContext bibDatabaseContext;

    @Mock
    private GuiPreferences preferences;
    @Mock
    private DuplicateCheck duplicateCheck;
    @Mock
    private DialogService dialogService;
    private StateManager stateManager;
    private BibEntry existingEntry;
    private BibEntry firstEntryToImport;
    private BibEntry secondEntryToImport;
    private CitationsRelationsTabViewModel viewModel;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        when(preferences.getImportFormatPreferences()).thenReturn(importFormatPreferences);

        ImporterPreferences importerPreferences = mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importerPreferences.shouldGenerateNewKeyOnImport()).thenReturn(false);
        when(preferences.getImporterPreferences()).thenReturn(importerPreferences);
        when(preferences.getImporterPreferences().getCustomImporters()).thenReturn(FXCollections.emptyObservableSet());

        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.observableArrayList());
        when(preferences.getFieldPreferences()).thenReturn(fieldPreferences);

        when(preferences.getFilePreferences()).thenReturn(mock(FilePreferences.class));
        when(preferences.getExternalApplicationsPreferences()).thenReturn(mock(ExternalApplicationsPreferences.class, Answers.RETURNS_DEEP_STUBS));
        when(preferences.getOwnerPreferences()).thenReturn(mock(OwnerPreferences.class, Answers.RETURNS_DEEP_STUBS));
        when(preferences.getTimestampPreferences()).thenReturn(mock(TimestampPreferences.class, Answers.RETURNS_DEEP_STUBS));

        CitationKeyPatternPreferences citationKeyPatternPreferences = mock(CitationKeyPatternPreferences.class);
        GlobalCitationKeyPatterns patterns = GlobalCitationKeyPatterns.fromPattern("[auth][year]");
        when(citationKeyPatternPreferences.getKeyPatterns()).thenReturn(patterns);
        when(preferences.getCitationKeyPatternPreferences()).thenReturn(citationKeyPatternPreferences);
        when(duplicateCheck.isDuplicate(any(), any(), any())).thenReturn(false);

        stateManager = mock(StateManager.class, Answers.RETURNS_DEEP_STUBS);
        // A real journal: the import records itself, and a deep-stubbed manager would answer the
        // recording block without running it, so nothing would be imported at all.
        when(stateManager.getUndoManager(any(BibDatabaseContext.class))).thenReturn(new HeadlessGuiUndoManager());
        bibDatabaseContext = new BibDatabaseContext(new BibDatabase());
        bibDatabaseContext.setMode(BibDatabaseMode.BIBTEX);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(bibDatabaseContext));
        // The duplicate-checked import selects the imported entries in the active tab; a deep-stubbed Optional would fail there.
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.empty());

        LibraryPreferences libraryPreferences = mock(LibraryPreferences.class);
        when(libraryPreferences.shouldAddImportedEntries()).thenReturn(false);
        when(preferences.getLibraryPreferences()).thenReturn(libraryPreferences);
        when(preferences.getMergeDialogPreferences()).thenReturn(mock(MergeDialogPreferences.class));

        viewModel = new CitationsRelationsTabViewModel(
                preferences,
                stateManager,
                dialogService,
                new DummyFileUpdateMonitor(),
                new CurrentThreadTaskExecutor());

        existingEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Test2023")
                .withField(StandardField.AUTHOR, "Test Author");

        bibDatabaseContext.getDatabase().insertEntry(existingEntry);

        firstEntryToImport = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "First Author")
                                                                    .withField(StandardField.YEAR, "2022")
                                                                    .withField(StandardField.TITLE, "Gut microbiota and cognition")
                                                                    .withCitationKey("FirstAuthorCitationKey2022");

        secondEntryToImport = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "Second Author")
                                                                     .withField(StandardField.YEAR, "2021")
                                                                     .withField(StandardField.TITLE, "Dietary flavanols and memory")
                                                                     .withCitationKey("SecondAuthorCitationKey20221");
    }

    @Test
    void existingEntryCitesOtherPaperWithCitationKeys() {
        List<CitationRelationItem> citationItems = List.of(
                new CitationRelationItem(firstEntryToImport, false),
                new CitationRelationItem(secondEntryToImport, false));

        viewModel.importEntries(citationItems, CitationFetcher.SearchType.CITES, existingEntry);

        assertEquals(Optional.of("FirstAuthorCitationKey2022,SecondAuthorCitationKey20221"), existingEntry.getField(StandardField.CITES));
        assertEquals(List.of(existingEntry, firstEntryToImport, secondEntryToImport), bibDatabaseContext.getEntries());
    }

    @Test
    void importedEntriesWithExistingCitationKeysCiteExistingEntry() {
        List<CitationRelationItem> citationItems = List.of(
                new CitationRelationItem(firstEntryToImport, false),
                new CitationRelationItem(secondEntryToImport, false));

        viewModel.importEntries(citationItems, CitationFetcher.SearchType.CITED_BY, existingEntry);

        // The entries are cloned during the import. Thus, we need to get the actual entries from the database.
        // In the test, the citation key is not changed during the import, thus we can just look up the entries by their citation key.
        BibEntry firstEntryInLibrary = bibDatabaseContext.getDatabase().getEntryByCitationKey(firstEntryToImport.getCitationKey().orElseThrow()).orElseThrow();
        BibEntry secondEntryInLibrary = bibDatabaseContext.getDatabase().getEntryByCitationKey(secondEntryToImport.getCitationKey().orElseThrow()).orElseThrow();

        assertEquals(Optional.of("Test2023"), firstEntryInLibrary.getField(StandardField.CITES));
        assertEquals(List.of(existingEntry, firstEntryInLibrary, secondEntryInLibrary), bibDatabaseContext.getEntries());
    }

    @Test
    void existingEntryCitesOtherPaperWithCitationKeysAndExistingCiteField() {
        existingEntry.setField(StandardField.CITES, "Asdf1222");
        List<CitationRelationItem> citationItems = List.of(
                new CitationRelationItem(firstEntryToImport, false),
                new CitationRelationItem(secondEntryToImport, false));

        viewModel.importEntries(citationItems, CitationFetcher.SearchType.CITES, existingEntry);

        assertEquals(Optional.of("Asdf1222,FirstAuthorCitationKey2022,SecondAuthorCitationKey20221"), existingEntry.getField(StandardField.CITES));
        assertEquals(List.of(existingEntry, firstEntryToImport, secondEntryToImport), bibDatabaseContext.getEntries());
    }

    @Test
    void importEntriesUpdatesPropertyOnSuccess() {
        List<CitationRelationItem> citationItems = List.of(
                new CitationRelationItem(firstEntryToImport, false));

        viewModel.importEntries(citationItems, CitationFetcher.SearchType.CITES, existingEntry);

        BibEntry lastImported = viewModel.lastImportedEntryProperty().get();

        assertNotNull(lastImported);
        assertEquals(firstEntryToImport.getAuthorTitleYear(), lastImported.getAuthorTitleYear());
    }

    @Test
    void importEntriesUpdatesEvenIfCitationKeyIsMissing() {
        existingEntry.clearCitationKey();
        List<CitationRelationItem> citationItems = List.of(
                new CitationRelationItem(firstEntryToImport, false));
        viewModel.importEntries(citationItems, CitationFetcher.SearchType.CITED_BY, existingEntry);
        assertEquals(citationItems.getFirst().entry(), viewModel.lastImportedEntryProperty().get());
    }

    // [utest->feat~entry-editor.citations.review-before-import~1]
    @Test
    void importReviewedEntries_addsCitationKeysToCitesField_whenSearchTypeIsCites() {
        List<CitationRelationItem> citationItems = List.of(
                new CitationRelationItem(firstEntryToImport, false),
                new CitationRelationItem(secondEntryToImport, false));

        viewModel.importReviewedEntries(citationItems, CitationFetcher.SearchType.CITES, existingEntry);

        assertEquals(Optional.of("FirstAuthorCitationKey2022,SecondAuthorCitationKey20221"), existingEntry.getField(StandardField.CITES));
    }

    @Test
    void importReviewedEntries_insertsEntriesToLibrary_whenSearchTypeIsCites() {
        List<CitationRelationItem> citationItems = List.of(
                new CitationRelationItem(firstEntryToImport, false),
                new CitationRelationItem(secondEntryToImport, false));

        viewModel.importReviewedEntries(citationItems, CitationFetcher.SearchType.CITES, existingEntry);

        // The duplicate check imports copies, so the library content is compared by citation key.
        assertEquals(List.of("Test2023", "FirstAuthorCitationKey2022", "SecondAuthorCitationKey20221"),
                bibDatabaseContext.getEntries().stream().map(entry -> entry.getCitationKey().orElseThrow()).toList());
    }

    // [utest->feat~entry-editor.citations.review-before-import~1]
    @Test
    void importReviewedEntries_setsCitesFieldOnImportedEntries_whenSearchTypeIsCitedBy() {
        List<CitationRelationItem> citationItems = List.of(
                new CitationRelationItem(firstEntryToImport, false),
                new CitationRelationItem(secondEntryToImport, false));

        viewModel.importReviewedEntries(citationItems, CitationFetcher.SearchType.CITED_BY, existingEntry);

        BibEntry firstEntryInLibrary = bibDatabaseContext.getDatabase().getEntryByCitationKey(firstEntryToImport.getCitationKey().orElseThrow()).orElseThrow();
        BibEntry secondEntryInLibrary = bibDatabaseContext.getDatabase().getEntryByCitationKey(secondEntryToImport.getCitationKey().orElseThrow()).orElseThrow();

        assertEquals(Optional.of("Test2023"), firstEntryInLibrary.getField(StandardField.CITES));
        assertEquals(Optional.of("Test2023"), secondEntryInLibrary.getField(StandardField.CITES));
    }

    @Test
    void importReviewedEntries_updatesLastImportedEntryProperty_whenSearchTypeIsCitedBy() {
        List<CitationRelationItem> citationItems = List.of(
                new CitationRelationItem(firstEntryToImport, false),
                new CitationRelationItem(secondEntryToImport, false));

        viewModel.importReviewedEntries(citationItems, CitationFetcher.SearchType.CITED_BY, existingEntry);

        BibEntry firstEntryInLibrary = bibDatabaseContext.getDatabase().getEntryByCitationKey(firstEntryToImport.getCitationKey().orElseThrow()).orElseThrow();
        assertEquals(firstEntryInLibrary, viewModel.lastImportedEntryProperty().get());
    }

    @Test
    void importReviewedEntries_doesNothing_whenEntriesToImportIsEmpty() {
        viewModel.importReviewedEntries(List.of(), CitationFetcher.SearchType.CITES, existingEntry);

        assertEquals(List.of(existingEntry), bibDatabaseContext.getEntries());
        assertEquals(Optional.empty(), existingEntry.getField(StandardField.CITES));
        verify(dialogService, never()).notify(any(String.class));
    }

    @Test
    void importReviewedEntries_notifiesUser_whenNoLibraryIsOpen() {
        when(stateManager.getActiveDatabase()).thenReturn(Optional.empty());
        List<CitationRelationItem> citationItems = List.of(new CitationRelationItem(firstEntryToImport, false));

        viewModel.importReviewedEntries(citationItems, CitationFetcher.SearchType.CITES, existingEntry);

        verify(dialogService).notify(Localization.lang("No library open"));
        assertEquals(Optional.empty(), existingEntry.getField(StandardField.CITES));
    }

    @Test
    void importReviewedEntries_appendsToExistingCitesField_whenExistingEntryAlreadyHasCites() {
        existingEntry.setField(StandardField.CITES, "ExistingCiteKey2020");
        List<CitationRelationItem> citationItems = List.of(
                new CitationRelationItem(firstEntryToImport, false),
                new CitationRelationItem(secondEntryToImport, false));

        viewModel.importReviewedEntries(citationItems, CitationFetcher.SearchType.CITES, existingEntry);

        assertEquals(Optional.of("ExistingCiteKey2020,FirstAuthorCitationKey2022,SecondAuthorCitationKey20221"),
                existingEntry.getField(StandardField.CITES));
    }

    @Test
    void importReviewedEntries_notifiesUser_whenInspectedEntryHasNoCitationKeyAndSearchTypeIsCitedBy() {
        existingEntry.clearCitationKey();
        List<CitationRelationItem> citationItems = List.of(new CitationRelationItem(firstEntryToImport, false));

        viewModel.importReviewedEntries(citationItems, CitationFetcher.SearchType.CITED_BY, existingEntry);

        verify(dialogService).notify(Localization.lang("No citation key for %0", existingEntry.getAuthorTitleYear()));
        BibEntry firstEntryInLibrary = bibDatabaseContext.getDatabase().getEntryByCitationKey(firstEntryToImport.getCitationKey().orElseThrow()).orElseThrow();
        assertEquals(Optional.empty(), firstEntryInLibrary.getField(StandardField.CITES));
    }

    @Test
    void importReviewedEntries_doesNotLinkEntry_whenDuplicateIsSkipped() {
        BibEntry duplicateInDb = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "First Author")
                .withField(StandardField.YEAR, "2022")
                .withField(StandardField.TITLE, "Gut microbiota and cognition")
                .withCitationKey("ExistingKey2022");
        bibDatabaseContext.getDatabase().insertEntry(duplicateInDb);

        when(dialogService.showCustomDialogAndWait(any()))
                .thenReturn(Optional.of(DuplicateResolverDialog.DuplicateResolverResult.KEEP_LEFT));

        List<CitationRelationItem> citationItems = List.of(
                new CitationRelationItem(firstEntryToImport, false),
                new CitationRelationItem(secondEntryToImport, false));

        JavaFxExtension.invokeAndWait(() ->
                viewModel.importReviewedEntries(citationItems, CitationFetcher.SearchType.CITES, existingEntry));

        // firstEntryToImport is a duplicate and was skipped, so only secondEntryToImport should be linked.
        assertEquals(Optional.of("SecondAuthorCitationKey20221"), existingEntry.getField(StandardField.CITES));
    }

    // [utest->feat~entry-editor.citations.review-before-import~1]
    @Test
    void importReviewedEntries_importsEditedFieldValues_leavingOriginalCitationItemUnchanged() {
        BibEntry originalFetched = new BibEntry(StandardEntryType.Article)
                .withCitationKey("FetchedKey2022")
                .withField(StandardField.AUTHOR, "Fetched Author")
                .withField(StandardField.TITLE, "Original Title");
        BibEntry editedCopy = new BibEntry(originalFetched)
                .withField(StandardField.TITLE, "Corrected Title during Review")
                .withField(StandardField.DOI, "10.1000/182");

        List<CitationRelationItem> citationItems = List.of(new CitationRelationItem(editedCopy, false));

        viewModel.importReviewedEntries(citationItems, CitationFetcher.SearchType.CITES, existingEntry);

        BibEntry importedEntry = bibDatabaseContext.getDatabase().getEntryByCitationKey("FetchedKey2022").orElseThrow();
        assertEquals(Optional.of("Corrected Title during Review"), importedEntry.getField(StandardField.TITLE));
        assertEquals(Optional.of("10.1000/182"), importedEntry.getField(StandardField.DOI));

        // Original fetched entry remains untouched
        assertEquals(Optional.of("Original Title"), originalFetched.getField(StandardField.TITLE));
        assertEquals(Optional.empty(), originalFetched.getField(StandardField.DOI));
    }

    @Test
    void updateForEntryWithoutDoiSetsDoiMissingStatus() {
        BibEntry entryWithoutDoi = new BibEntry(StandardEntryType.Article);
        viewModel.updateForEntry(entryWithoutDoi);
        assertEquals(CitationsRelationsTabViewModel.SciteStatus.DOI_MISSING, viewModel.statusProperty().get());
    }

    @Test
    void updateForNullEntrySetsErrorStatus() {
        viewModel.updateForEntry(null);
        assertEquals(CitationsRelationsTabViewModel.SciteStatus.ERROR, viewModel.statusProperty().get());
    }

    @Test
    void updateForEntryTransitionFromNoDoiToDoiResetsStatus() {
        BibEntry entryWithoutDoi = new BibEntry(StandardEntryType.Article);
        viewModel.updateForEntry(entryWithoutDoi);
        assertEquals(CitationsRelationsTabViewModel.SciteStatus.DOI_MISSING, viewModel.statusProperty().get());

        BibEntry entryWithDoi = new BibEntry(StandardEntryType.Article).withField(StandardField.DOI, "10.1000/182");
        viewModel.updateForEntry(entryWithDoi);
        assertNotEquals(CitationsRelationsTabViewModel.SciteStatus.DOI_MISSING, viewModel.statusProperty().get());
    }

    @Test
    void trackCitationSearchCancelsPreviousTaskOfSameType() {
        BackgroundTask<List<CitationRelationItem>> initialTask = mock(BackgroundTask.class);
        BackgroundTask<List<CitationRelationItem>> newTask = mock(BackgroundTask.class);

        viewModel.trackCitationSearch(CitationFetcher.SearchType.CITES, initialTask);
        verify(initialTask, never()).cancel(any(Boolean.class));

        viewModel.trackCitationSearch(CitationFetcher.SearchType.CITES, newTask);
        verify(initialTask).cancel(false);
        verify(newTask, never()).cancel(any(Boolean.class));
    }

    @Test
    void trackCitationSearchDoesNotCancelOtherSearchTypeTask() {
        BackgroundTask<List<CitationRelationItem>> citesTask = mock(BackgroundTask.class);
        BackgroundTask<List<CitationRelationItem>> citedByTask = mock(BackgroundTask.class);

        viewModel.trackCitationSearch(CitationFetcher.SearchType.CITES, citesTask);
        viewModel.trackCitationSearch(CitationFetcher.SearchType.CITED_BY, citedByTask);

        verify(citesTask, never()).cancel(any(Boolean.class));
        verify(citedByTask, never()).cancel(any(Boolean.class));
    }

    @Test
    void cancelCitationSearchesCancelsBothTasks() {
        BackgroundTask<List<CitationRelationItem>> citesTask = mock(BackgroundTask.class);
        BackgroundTask<List<CitationRelationItem>> citedByTask = mock(BackgroundTask.class);

        viewModel.trackCitationSearch(CitationFetcher.SearchType.CITES, citesTask);
        viewModel.trackCitationSearch(CitationFetcher.SearchType.CITED_BY, citedByTask);

        viewModel.cancelCitationSearches();

        verify(citesTask).cancel(false);
        verify(citedByTask).cancel(false);
    }
}
