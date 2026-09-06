package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CitationsRelationsTabViewModelTest {
    private BibDatabaseContext bibDatabaseContext;

    @Mock
    private GuiPreferences preferences;
    @Mock
    private DuplicateCheck duplicateCheck;
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

        StateManager stateManager = mock(StateManager.class, Answers.RETURNS_DEEP_STUBS);
        bibDatabaseContext = new BibDatabaseContext(new BibDatabase());
        bibDatabaseContext.setMode(BibDatabaseMode.BIBTEX);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(bibDatabaseContext));

        LibraryPreferences libraryPreferences = mock(LibraryPreferences.class);
        when(libraryPreferences.shouldAddImportedEntries()).thenReturn(false);
        when(preferences.getLibraryPreferences()).thenReturn(libraryPreferences);

        viewModel = new CitationsRelationsTabViewModel(
                preferences,
                stateManager,
                mock(DialogService.class),
                new DummyFileUpdateMonitor(),
                new CurrentThreadTaskExecutor());

        existingEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Test2023")
                .withField(StandardField.AUTHOR, "Test Author");

        bibDatabaseContext.getDatabase().insertEntry(existingEntry);

        firstEntryToImport = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "First Author")
                                                                    .withField(StandardField.YEAR, "2022")
                                                                    .withCitationKey("FirstAuthorCitationKey2022");

        secondEntryToImport = new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "Second Author")
                                                                     .withField(StandardField.YEAR, "2021")
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
        BibEntry firstEntryInLibrary = bibDatabaseContext.getDatabase().getEntryByCitationKey(firstEntryToImport.getCitationKey().get()).get();
        BibEntry secondEntryInLibrary = bibDatabaseContext.getDatabase().getEntryByCitationKey(secondEntryToImport.getCitationKey().get()).get();

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
        BackgroundTask<List<CitationRelationItem>> firstTask = BackgroundTask.wrap(() -> List.<CitationRelationItem>of());
        BackgroundTask<List<CitationRelationItem>> secondTask = BackgroundTask.wrap(() -> List.<CitationRelationItem>of());

        viewModel.trackCitationSearch(CitationFetcher.SearchType.CITES, firstTask);
        viewModel.trackCitationSearch(CitationFetcher.SearchType.CITES, secondTask);

        assertTrue(firstTask.isCancelled());
        assertTrue(viewModel.isTrackedCitationSearch(CitationFetcher.SearchType.CITES, secondTask));
        assertFalse(viewModel.isTrackedCitationSearch(CitationFetcher.SearchType.CITES, firstTask));
    }

    @Test
    void cancelCitationSearchesCancelsTrackedTasks() {
        BackgroundTask<List<CitationRelationItem>> citesTask = BackgroundTask.wrap(() -> List.<CitationRelationItem>of());
        BackgroundTask<List<CitationRelationItem>> citedByTask = BackgroundTask.wrap(() -> List.<CitationRelationItem>of());

        viewModel.trackCitationSearch(CitationFetcher.SearchType.CITES, citesTask);
        viewModel.trackCitationSearch(CitationFetcher.SearchType.CITED_BY, citedByTask);

        viewModel.cancelCitationSearches();

        assertTrue(citesTask.isCancelled());
        assertTrue(citedByTask.isCancelled());
    }
}
