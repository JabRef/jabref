package org.jabref.gui.externalfiles;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.swing.undo.UndoManager;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImportFormatReader.ImportResult;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
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
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportHandlerTest {

    private ImportHandler importHandler;
    private BibDatabaseContext bibDatabaseContext;
    private BibEntry testEntry;

    @Mock
    private GuiPreferences preferences;
    @Mock
    private DuplicateCheck duplicateCheck;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getImportFormatPreferences()).thenReturn(importFormatPreferences);
        when(preferences.getFilePreferences()).thenReturn(mock(FilePreferences.class));
        when(preferences.getExternalApplicationsPreferences()).thenReturn(mock(ExternalApplicationsPreferences.class, Answers.RETURNS_DEEP_STUBS));
        when(preferences.getImporterPreferences()).thenReturn(mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS));
        when(preferences.getCitationKeyPatternPreferences()).thenReturn(mock(CitationKeyPatternPreferences.class, Answers.RETURNS_DEEP_STUBS));
        when(preferences.getImporterPreferences().getCustomImporters()).thenReturn(FXCollections.emptyObservableSet());

        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.observableArrayList());
        when(preferences.getFieldPreferences()).thenReturn(fieldPreferences);

        when(preferences.getOwnerPreferences()).thenReturn(mock(OwnerPreferences.class, Answers.RETURNS_DEEP_STUBS));
        when(preferences.getTimestampPreferences()).thenReturn(mock(TimestampPreferences.class, Answers.RETURNS_DEEP_STUBS));
        when(preferences.getLibraryPreferences()).thenReturn(mock(LibraryPreferences.class, Answers.RETURNS_DEEP_STUBS));

        bibDatabaseContext = mock(BibDatabaseContext.class);
        BibDatabase bibDatabase = new BibDatabase();
        when(bibDatabaseContext.getMode()).thenReturn(BibDatabaseMode.BIBTEX);
        when(bibDatabaseContext.getDatabase()).thenReturn(bibDatabase);
        when(duplicateCheck.isDuplicate(any(), any(), any())).thenReturn(false);
        importHandler = new ImportHandler(
                bibDatabaseContext,
                preferences,
                new DummyFileUpdateMonitor(),
                mock(UndoManager.class),
                mock(StateManager.class),
                mock(DialogService.class),
                new CurrentThreadTaskExecutor());

        testEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Test2023")
                .withField(StandardField.AUTHOR, "Test Author");
    }

    @Test
    void handleBibTeXData() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);

        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getImportFormatPreferences()).thenReturn(importFormatPreferences);
        when(preferences.getFilePreferences()).thenReturn(mock(FilePreferences.class));
        when(preferences.getExternalApplicationsPreferences()).thenReturn(mock(ExternalApplicationsPreferences.class, Answers.RETURNS_DEEP_STUBS));
        when(preferences.getImporterPreferences()).thenReturn(mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS));
        when(preferences.getCitationKeyPatternPreferences()).thenReturn(mock(CitationKeyPatternPreferences.class, Answers.RETURNS_DEEP_STUBS));
        when(preferences.getImporterPreferences().getCustomImporters()).thenReturn(FXCollections.emptyObservableSet());

        ImportHandler importHandler = new ImportHandler(
                mock(BibDatabaseContext.class),
                preferences,
                new DummyFileUpdateMonitor(),
                mock(UndoManager.class),
                mock(StateManager.class),
                mock(DialogService.class),
                new CurrentThreadTaskExecutor());

        List<BibEntry> bibEntries = importHandler.handleBibTeXData("""
                @InProceedings{Wen2013,
                  library          = {Tagungen\\2013\\KWTK45\\},
                }
                """);

        BibEntry expected = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("Wen2013")
                .withField(StandardField.LIBRARY, "Tagungen\\2013\\KWTK45\\");

        assertEquals(List.of(expected), bibEntries.stream().toList());
    }

    @Test
    void cleanUpEntryTest() {
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Clear Author");
        BibEntry cleanedEntry = importHandler.cleanUpEntry(entry);
        assertEquals(new BibEntry().withField(StandardField.AUTHOR, "Clear Author"), cleanedEntry);
    }

    @Test
    void createAutoDetectionImportOutcomeReturnsWarningResultForParsedFileWithWarnings() {
        BibEntry importedEntry = new BibEntry(StandardEntryType.Article).withCitationKey("Warning2026");
        ParserResult parserResult = new ParserResult(List.of(importedEntry));
        parserResult.addWarning("Warning text");

        ImportResult importResult = new ImportResult("RIS", parserResult);

        ImportHandler.AutoDetectionImportOutcome outcome = importHandler.createAutoDetectionImportOutcome(Path.of("sample.ris"), importResult);

        assertFalse(outcome.success());
        assertEquals(List.of(importedEntry), outcome.entriesToAdd());
        assertEquals("File was imported as RIS, but warnings were reported: Warning text", outcome.message());
    }

    @Test
    void createAutoDetectionImportOutcomeReturnsEmptyEntryAndWarningMessageWhenNoEntriesWereParsed() {
        ParserResult parserResult = new ParserResult();
        parserResult.addWarning("Warning text");

        ImportResult importResult = new ImportResult("RIS", parserResult);

        ImportHandler.AutoDetectionImportOutcome outcome = importHandler.createAutoDetectionImportOutcome(Path.of("sample.ris"), importResult);

        assertFalse(outcome.success());
        assertEquals(1, outcome.entriesToAdd().size());
        assertEquals("No importable data was found in RIS. An empty entry was created with file link. Warning text", outcome.message());
    }

    @Test
    void findDuplicateTest() {
        // Assume there is no duplicate initially
        assertTrue(importHandler.findDuplicate(testEntry).isEmpty());
    }

    @Test
    void importWithDuplicateCheckTracksInsertedCopyForGroupAssignment() {
        // Use a real database context so group metadata can be created/queried
        BibDatabaseContext realContext = new BibDatabaseContext(new BibDatabase());
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedGroups(any())).thenReturn(FXCollections.observableArrayList());
        ImportHandler handler = new ImportHandler(
                realContext,
                preferences,
                new DummyFileUpdateMonitor(),
                mock(UndoManager.class),
                stateManager,
                mock(DialogService.class),
                new CurrentThreadTaskExecutor());

        EntryImportHandlerTracker tracker = new EntryImportHandlerTracker(stateManager, 1);
        handler.importEntriesWithDuplicateCheck(null, List.of(testEntry), tracker);

        // The tracker exposes the actual inserted copy, not the original
        List<BibEntry> imported = tracker.getImportedEntries();
        assertEquals(1, imported.size());
        BibEntry insertedEntry = realContext.getDatabase().getEntries().getFirst();
        assertSame(insertedEntry, imported.getFirst());
        // The tracked/inserted entry is a copy, not the caller's original
        assertNotSame(testEntry, insertedEntry);

        // Assigning the group to the tracked entries puts the actual database entry into the group
        org.jabref.logic.groups.GroupsHelper.assignEntriesToGroup(realContext, imported, "MyGroup", ',');

        assertTrue(insertedEntry.getField(StandardField.GROUPS).orElse("").contains("MyGroup"));
        // The original entry passed by the caller is untouched
        assertTrue(testEntry.getField(StandardField.GROUPS).isEmpty());
    }

    @Test
    void handleDuplicatesKeepRightTest() {
        // Arrange
        BibEntry duplicateEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Duplicate2023")
                .withField(StandardField.AUTHOR, "Duplicate Author");

        BibDatabase bibDatabase = bibDatabaseContext.getDatabase();
        bibDatabase.insertEntry(duplicateEntry); // Simulate that the duplicate entry is already in the database

        DuplicateDecisionResult decisionResult = new DuplicateDecisionResult(DuplicateResolverDialog.DuplicateResolverResult.KEEP_RIGHT, null);
        importHandler = Mockito.spy(new ImportHandler(
                bibDatabaseContext,
                preferences,
                new DummyFileUpdateMonitor(),
                mock(UndoManager.class),
                mock(StateManager.class),
                mock(DialogService.class),
                new CurrentThreadTaskExecutor()));
        // Mock the behavior of getDuplicateDecision to return KEEP_RIGHT
        Mockito.doReturn(CompletableFuture.completedFuture(decisionResult)).when(importHandler).getDuplicateDecision(testEntry, duplicateEntry, DuplicateResolverDialog.DuplicateResolverResult.BREAK);

        // Act
        BibEntry result = importHandler.handleDuplicates(testEntry, duplicateEntry, DuplicateResolverDialog.DuplicateResolverResult.BREAK).join().get();

        // Assert that the duplicate entry was removed from the database
        assertFalse(bibDatabase.getEntries().contains(duplicateEntry));
        // Assert that the original entry is returned
        assertEquals(testEntry, result);
    }

    @Test
    void handleDuplicatesKeepBothTest() {
        // Arrange
        BibEntry duplicateEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Duplicate2023")
                .withField(StandardField.AUTHOR, "Duplicate Author");

        BibDatabase bibDatabase = bibDatabaseContext.getDatabase();
        bibDatabase.insertEntry(duplicateEntry); // Simulate that the duplicate entry is already in the database

        DuplicateDecisionResult decisionResult = new DuplicateDecisionResult(DuplicateResolverDialog.DuplicateResolverResult.KEEP_BOTH, null);
        importHandler = Mockito.spy(new ImportHandler(
                bibDatabaseContext,
                preferences,
                new DummyFileUpdateMonitor(),
                mock(UndoManager.class),
                mock(StateManager.class),
                mock(DialogService.class),
                new CurrentThreadTaskExecutor()));
        // Mock the behavior of getDuplicateDecision to return KEEP_BOTH
        Mockito.doReturn(CompletableFuture.completedFuture(decisionResult)).when(importHandler).getDuplicateDecision(testEntry, duplicateEntry, DuplicateResolverDialog.DuplicateResolverResult.BREAK);

        // Act
        BibEntry result = importHandler.handleDuplicates(testEntry, duplicateEntry, DuplicateResolverDialog.DuplicateResolverResult.BREAK).join().get();

        // Assert
        assertTrue(bibDatabase.getEntries().contains(duplicateEntry)); // Assert that the duplicate entry is still in the database
        assertEquals(testEntry, result); // Assert that the original entry is returned
    }

    @Test
    void handleDuplicatesKeepMergeTest() {
        // Arrange
        BibEntry duplicateEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Duplicate2023")
                .withField(StandardField.AUTHOR, "Duplicate Author");

        BibEntry mergedEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Merged2023")
                .withField(StandardField.AUTHOR, "Merged Author");

        BibDatabase bibDatabase = bibDatabaseContext.getDatabase();
        bibDatabase.insertEntry(duplicateEntry); // Simulate that the duplicate entry is already in the database

        DuplicateDecisionResult decisionResult = new DuplicateDecisionResult(DuplicateResolverDialog.DuplicateResolverResult.KEEP_MERGE, mergedEntry);
        importHandler = Mockito.spy(new ImportHandler(
                bibDatabaseContext,
                preferences,
                new DummyFileUpdateMonitor(),
                mock(UndoManager.class),
                mock(StateManager.class),
                mock(DialogService.class),
                new CurrentThreadTaskExecutor()));
        // Mock the behavior of getDuplicateDecision to return KEEP_MERGE
        Mockito.doReturn(CompletableFuture.completedFuture(decisionResult)).when(importHandler).getDuplicateDecision(testEntry, duplicateEntry, DuplicateResolverDialog.DuplicateResolverResult.BREAK);

        // Act
        // create and return a default BibEntry or do other computations
        BibEntry result = importHandler.handleDuplicates(testEntry, duplicateEntry, DuplicateResolverDialog.DuplicateResolverResult.BREAK).join()
                                       .orElseGet(BibEntry::new);

        // Assert
        assertFalse(bibDatabase.getEntries().contains(duplicateEntry)); // Assert that the duplicate entry was removed from the database
        assertEquals(mergedEntry, result); // Assert that the merged entry is returned
    }

    @Test
    void confirmBibFileImportReturnsTrueWhenNoBibFiles() {
        DialogService dialogService = mock(DialogService.class);
        List<Path> files = List.of(Path.of("paper.pdf"));
        assertTrue(ImportHandler.confirmBibFileImportIfNecessary(files, preferences, dialogService));
    }

    @Test
    void confirmBibFileImportShowsDialogWhenBibFileDropped() {
        DialogService dialogService = mock(DialogService.class);
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
        when(preferences.getImporterPreferences()).thenReturn(importerPreferences);
        when(importerPreferences.shouldWarnAboutBibFileImport()).thenReturn(true);
        when(dialogService.showConfirmationDialogWithOptOutAndWait(any(), any(), any(), any(), any(), any())).thenReturn(true);

        List<Path> files = List.of(Path.of("library.bib"));
        boolean result = ImportHandler.confirmBibFileImportIfNecessary(files, preferences, dialogService);

        assertTrue(result);
    }
}
