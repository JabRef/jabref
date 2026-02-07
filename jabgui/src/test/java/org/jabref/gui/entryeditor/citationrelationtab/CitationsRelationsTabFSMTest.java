package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.citationrelationtab.CitationsRelationsTabViewModel.SciteStatus;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.SciteAiFetcher;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.sciteTallies.TalliesResponse;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the finite state machine (FSM) of the Citation Relations Tab.
 * 6 states: IN_PROGRESS, FOUND, ERROR, DOI_MISSING, DOI_LOOK_UP, DOI_LOOK_UP_ERROR
 * 
 * FSM transitions tested:
 * 1. null entry -> ERROR
 * 2. entry without DOI -> DOI_MISSING
 * 3. entry with DOI (success) -> IN_PROGRESS -> FOUND
 * 4. entry with DOI (failure) -> IN_PROGRESS -> ERROR
 * 5. DOI_MISSING -> lookUpDoi (DOI found) -> DOI_LOOK_UP -> IN_PROGRESS -> FOUND
 * 6. DOI_MISSING -> lookUpDoi (no DOI found) -> DOI_LOOK_UP -> DOI_MISSING
 * 7. DOI_MISSING -> lookUpDoi (lookup fails) -> DOI_LOOK_UP -> DOI_LOOK_UP_ERROR
 */
class CitationsRelationsTabFSMTest {

    private GuiPreferences preferences;
    private StateManager stateManager;
    private BibDatabaseContext bibDatabaseContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getImportFormatPreferences()).thenReturn(importFormatPreferences);

        ImporterPreferences importerPreferences = mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importerPreferences.shouldGenerateNewKeyOnImport()).thenReturn(false);
        when(preferences.getImporterPreferences()).thenReturn(importerPreferences);

        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.observableArrayList());
        when(preferences.getFieldPreferences()).thenReturn(fieldPreferences);

        when(preferences.getFilePreferences()).thenReturn(mock(FilePreferences.class));
        when(preferences.getOwnerPreferences()).thenReturn(mock(OwnerPreferences.class, Answers.RETURNS_DEEP_STUBS));
        when(preferences.getTimestampPreferences()).thenReturn(mock(TimestampPreferences.class, Answers.RETURNS_DEEP_STUBS));

        CitationKeyPatternPreferences citationKeyPatternPreferences = mock(CitationKeyPatternPreferences.class);
        GlobalCitationKeyPatterns patterns = GlobalCitationKeyPatterns.fromPattern("[auth][year]");
        when(citationKeyPatternPreferences.getKeyPatterns()).thenReturn(patterns);
        when(preferences.getCitationKeyPatternPreferences()).thenReturn(citationKeyPatternPreferences);

        stateManager = mock(StateManager.class, Answers.RETURNS_DEEP_STUBS);
        bibDatabaseContext = new BibDatabaseContext(new BibDatabase());
        bibDatabaseContext.setMode(BibDatabaseMode.BIBTEX);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(bibDatabaseContext));

        LibraryPreferences libraryPreferences = mock(LibraryPreferences.class);
        when(libraryPreferences.isAddImportedEntriesEnabled()).thenReturn(false);
        when(preferences.getLibraryPreferences()).thenReturn(libraryPreferences);
    }

    private CitationsRelationsTabViewModel createViewModel() {
        return new CitationsRelationsTabViewModel(
                preferences,
                mock(UndoManager.class),
                stateManager,
                mock(DialogService.class),
                new DummyFileUpdateMonitor(),
                new CurrentThreadTaskExecutor());
    }


    // Transition 1: null entry -> ERROR
    @Test
    void bindToNullEntryTransitionsToError() {
        try (MockedConstruction<SciteAiFetcher> ignored = Mockito.mockConstruction(SciteAiFetcher.class)) {
            CitationsRelationsTabViewModel viewModel = createViewModel();

            viewModel.bindToEntry(null);

            assertEquals(SciteStatus.ERROR, viewModel.statusProperty().get());
        }
    }

    // Transition 2: entry without DOI -> DOI_MISSING
    @Test
    void bindToEntryWithoutDoiTransitionsToDoiMissing() {
        try (MockedConstruction<SciteAiFetcher> ignored = Mockito.mockConstruction(SciteAiFetcher.class)) {
            CitationsRelationsTabViewModel viewModel = createViewModel();

            BibEntry entryWithoutDoi = new BibEntry(StandardEntryType.Article)
                    .withField(StandardField.AUTHOR, "Author Name");

            viewModel.bindToEntry(entryWithoutDoi);

            assertEquals(SciteStatus.DOI_MISSING, viewModel.statusProperty().get());
        }
    }

    // Transition 3: entry with DOI, fetch succeeds -> IN_PROGRESS -> FOUND
    @Test
    void bindToEntryWithDoiFetchSuccessTransitionsToFound() {
        TalliesResponse mockResponse = new TalliesResponse("10.1234/test", 10, 5, 2, 3, 0, 8);

        try (MockedConstruction<SciteAiFetcher> ignored = Mockito.mockConstruction(SciteAiFetcher.class,
                (mock, context) -> {
                    when(mock.fetchTallies(any(DOI.class))).thenReturn(mockResponse);
                })) {
            CitationsRelationsTabViewModel viewModel = createViewModel();

            BibEntry entryWithDoi = new BibEntry(StandardEntryType.Article)
                    .withField(StandardField.DOI, "10.1234/test");

            viewModel.bindToEntry(entryWithDoi);

            assertEquals(SciteStatus.FOUND, viewModel.statusProperty().get());
            assertEquals(Optional.of(mockResponse), viewModel.getCurrentResult());
        }
    }

    // Transition 4: entry with DOI, fetch fails -> IN_PROGRESS -> ERROR
    @Test
    void bindToEntryWithDoiFetchFailureTransitionsToError() {
        try (MockedConstruction<SciteAiFetcher> ignored = Mockito.mockConstruction(SciteAiFetcher.class,
                (mock, context) -> {
                    when(mock.fetchTallies(any(DOI.class))).thenThrow(new FetcherException("Network error"));
                })) {
            CitationsRelationsTabViewModel viewModel = createViewModel();

            BibEntry entryWithDoi = new BibEntry(StandardEntryType.Article)
                    .withField(StandardField.DOI, "10.1234/test");

            viewModel.bindToEntry(entryWithDoi);

            assertEquals(SciteStatus.ERROR, viewModel.statusProperty().get());
        }
    }

    // Transition 5: DOI_MISSING -> lookUpDoi (DOI found) -> DOI_LOOK_UP -> IN_PROGRESS -> FOUND
    @Test
    void lookUpDoiSuccessTransitionsThroughDoiLookUpToFound() {
        TalliesResponse mockResponse = new TalliesResponse("10.1234/found", 5, 3, 1, 1, 0, 4);
        DOI foundDoi = DOI.parse("10.1234/found").get();

        try (MockedConstruction<SciteAiFetcher> ignoredScite = Mockito.mockConstruction(SciteAiFetcher.class,
                (mock, context) -> {
                    when(mock.fetchTallies(any(DOI.class))).thenReturn(mockResponse);
                });
             MockedConstruction<CrossRef> ignoredCrossRef = Mockito.mockConstruction(CrossRef.class,
                (mock, context) -> {
                    when(mock.findIdentifier(any(BibEntry.class))).thenReturn(Optional.of(foundDoi));
                })) {

            CitationsRelationsTabViewModel viewModel = createViewModel();

            BibEntry entryWithoutDoi = new BibEntry(StandardEntryType.Article)
                    .withField(StandardField.AUTHOR, "Author Name")
                    .withField(StandardField.TITLE, "Title");

            viewModel.bindToEntry(entryWithoutDoi);
            assertEquals(SciteStatus.DOI_MISSING, viewModel.statusProperty().get());

            viewModel.lookUpDoi(entryWithoutDoi);
            assertEquals(SciteStatus.FOUND, viewModel.statusProperty().get());

            assertEquals(Optional.of("10.1234/found"), entryWithoutDoi.getField(StandardField.DOI));
        }
    }

    // Transition 6: DOI_MISSING -> lookUpDoi (no DOI found) -> DOI_LOOK_UP -> DOI_MISSING
    @Test
    void lookUpDoiFindsNothingTransitionsBackToDoiMissing() {
        try (MockedConstruction<SciteAiFetcher> ignoredScite = Mockito.mockConstruction(SciteAiFetcher.class);
             MockedConstruction<CrossRef> ignoredCrossRef = Mockito.mockConstruction(CrossRef.class,
                (mock, context) -> {
                    when(mock.findIdentifier(any(BibEntry.class))).thenReturn(Optional.empty());
                })) {

            CitationsRelationsTabViewModel viewModel = createViewModel();

            BibEntry entryWithoutDoi = new BibEntry(StandardEntryType.Article)
                    .withField(StandardField.AUTHOR, "Author Name");

            viewModel.bindToEntry(entryWithoutDoi);
            assertEquals(SciteStatus.DOI_MISSING, viewModel.statusProperty().get());

            viewModel.lookUpDoi(entryWithoutDoi);
            assertEquals(SciteStatus.DOI_MISSING, viewModel.statusProperty().get());
        }
    }

    // Transition 7: DOI_MISSING -> lookUpDoi (lookup fails) -> DOI_LOOK_UP -> DOI_LOOK_UP_ERROR
    @Test
    void lookUpDoiFailureTransitionsToDoiLookUpError() {
        try (MockedConstruction<SciteAiFetcher> ignoredScite = Mockito.mockConstruction(SciteAiFetcher.class);
             MockedConstruction<CrossRef> ignoredCrossRef = Mockito.mockConstruction(CrossRef.class,
                (mock, context) -> {
                    when(mock.findIdentifier(any(BibEntry.class))).thenThrow(new FetcherException("CrossRef unavailable"));
                })) {

            CitationsRelationsTabViewModel viewModel = createViewModel();

            BibEntry entryWithoutDoi = new BibEntry(StandardEntryType.Article)
                    .withField(StandardField.AUTHOR, "Author Name");

            viewModel.bindToEntry(entryWithoutDoi);
            assertEquals(SciteStatus.DOI_MISSING, viewModel.statusProperty().get());

            viewModel.lookUpDoi(entryWithoutDoi);
            assertEquals(SciteStatus.DOI_LOOK_UP_ERROR, viewModel.statusProperty().get());
        }
    }
}
