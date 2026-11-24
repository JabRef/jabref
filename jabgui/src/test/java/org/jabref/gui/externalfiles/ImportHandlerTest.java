package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.input.TransferMode;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.mergeentries.MergeDialogPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImportFormatReader.ImportResult;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.CitedEntryCollector;
import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.logic.openoffice.engine.CitedEntryExporter;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.undo.JabRefUndoManager;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ImportHandlerTest {

    private static final String NATIVE_ZOTERO_REFERENCE_MARK = ZoteroReferenceMark.PREFIX + """
            {
              "citationItems": [{
                "id": 587,
                "uris": ["http://zotero.org/users/123/items/ABCD1234"],
                "itemData": {
                  "id": 587,
                  "type": "article-journal",
                  "title": "Quantum transport in graphene",
                  "DOI": "10.1234/native.2024",
                  "author": [{"family": "Smith", "given": "Jane"}],
                  "issued": {"date-parts": [[2024]]}
                }
              }]
            }
            """ + " RNDabcdefghij";

    private ImportHandler importHandler;
    private BibDatabaseContext bibDatabaseContext;
    private BibEntry testEntry;

    @Mock
    private GuiPreferences preferences;
    @Mock
    private DuplicateCheck duplicateCheck;
    @Mock
    private DialogService dialogService;

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
                new JabRefUndoManager(),
                mock(StateManager.class),
                dialogService,
                new CurrentThreadTaskExecutor());

        testEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Test2023")
                .withField(StandardField.AUTHOR, "Test Author");
    }

    @ParameterizedTest
    @CsvSource({"true, false", "false, true"})
    void downloadOverrideControlsDownloadsWithoutChangingWebSearch(boolean choice, boolean webSearchChoice) {
        // [utest->req~import.dialog.download-linked-files~1]
        when(preferences.getFilePreferences().shouldDownloadLinkedFiles()).thenReturn(webSearchChoice);
        if (choice) {
            importHandler.enableLinkedFileDownloads();
        } else {
            importHandler.disableLinkedFileDownloads();
        }
        BibEntry entry = new BibEntry().withFiles(List.of(new LinkedFile("", "https://example.org/paper.pdf", "PDF")));

        try (MockedConstruction<LinkedFileViewModel> downloads = mockConstruction(LinkedFileViewModel.class)) {
            importHandler.downloadLinkedFiles(entry);

            assertEquals(choice ? 1 : 0, downloads.constructed().size());
            downloads.constructed().forEach(download -> verify(download).download(Mockito.eq(false), any()));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void downloadsUseWebSearchPreferenceWithoutOverride(boolean choice) {
        // [utest->req~import.dialog.download-linked-files~1]
        when(preferences.getFilePreferences().shouldDownloadLinkedFiles()).thenReturn(choice);
        BibEntry entry = new BibEntry().withFiles(List.of(new LinkedFile("", "https://example.org/paper.pdf", "PDF")));

        try (MockedConstruction<LinkedFileViewModel> downloads = mockConstruction(LinkedFileViewModel.class)) {
            importHandler.downloadLinkedFiles(entry);

            assertEquals(choice ? 1 : 0, downloads.constructed().size());
            downloads.constructed().forEach(download -> verify(download).download(Mockito.eq(false), any()));
        }
    }

    @Test
        // [utest->req~import.dropped-file-diagnostics~1]
    void importErrorsShowOnlyUnsuccessfulResults() {
        importHandler.showImportErrors(List.of(
                new ImportFilesResultItemViewModel(Path.of("good.bib"), true, "Imported"),
                new ImportFilesResultItemViewModel(Path.of("bad.bib"), false, "Invalid entry"),
                new ImportFilesResultItemViewModel(Path.of("other.bib"), ImportFilesResultItemViewModel.Status.WARNING, "Imported with warnings")));

        verify(dialogService).showWarningDialogAndWait("Import warnings", "Check the syntax and encoding of the BibTeX file and correct any errors.\n\nbad.bib\nother.bib\n\nSee the event log for details. Some entries may have been imported.");
    }

    @Test
    void nonBibtexImportErrorsSuggestCheckingTheFile() {
        importHandler.showImportErrors(List.of(new ImportFilesResultItemViewModel(Path.of("bad.ris"), false, "Invalid format")));

        verify(dialogService).showWarningDialogAndWait("Import warnings", "Check the affected files and correct any errors.\n\nbad.ris\n\nSee the event log for details. Some entries may have been imported.");
    }

    @Test
    void longImportDiagnosticsAreShortened() {
        List<ImportFilesResultItemViewModel> results = java.util.stream.IntStream.range(0, 20)
                                                                                 .mapToObj(index -> new ImportFilesResultItemViewModel(Path.of("file" + index + ".bib"), false, "x".repeat(2000)))
                                                                                 .toList();

        importHandler.showImportErrors(results);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(dialogService).showWarningDialogAndWait(Mockito.eq("Import warnings"), message.capture());
        assertTrue(message.getValue().length() < 500);
        assertTrue(message.getValue().contains("Additional files are listed in the event log."));
        assertFalse(message.getValue().contains("x".repeat(50)));
        assertFalse(message.getValue().contains("file5.bib"));
    }

    @Test
    void unexpectedImportFailureShowsError() {
        IOException failure = new IOException("Cannot read file");

        importHandler.showImportFailure(failure);

        verify(dialogService).showErrorDialogAndWait("Import error", failure);
    }

    @Test
    void successfulImportsDoNotShowWarning() {
        importHandler.showImportErrors(List.of(new ImportFilesResultItemViewModel(Path.of("good.bib"), true, "Imported")));

        Mockito.verifyNoInteractions(dialogService);
    }

    @Test
    @ExtendWith(JavaFxExtension.class)
    void cancellingPdfMergeDoesNotCreateAnEntryOrShowWarning() throws Exception {
        when(bibDatabaseContext.getDatabasePath()).thenReturn(Optional.empty());
        ImportHandler handler = Mockito.spy(importHandler);
        Mockito.doReturn(Optional.empty()).when(handler).showPdfMergeDialog(Path.of("cancelled.pdf"));
        Mockito.doNothing().when(handler).importEntries(any());

        List<ImportFilesResultItemViewModel> results = handler.importFilesInBackground(List.of(Path.of("cancelled.pdf")), TransferMode.COPY).call();
        JavaFxExtension.awaitEvents();

        assertEquals(List.of(), results);
        verify(handler).importEntries(List.of());
        Mockito.verifyNoInteractions(dialogService);
    }

    @Test
    @ExtendWith(JavaFxExtension.class)
    void droppedInvalidBibFileShowsWarning(@TempDir Path directory) throws IOException {
        when(bibDatabaseContext.getDatabasePath()).thenReturn(Optional.empty());
        ImportHandler handler = Mockito.spy(importHandler);
        Mockito.doNothing().when(handler).importEntries(any());
        Path file = directory.resolve("invalid.bib");
        Files.writeString(file, "@article{broken, title={unclosed}");

        handler.importDroppedFiles(List.of(file), TransferMode.COPY);
        JavaFxExtension.awaitEvents();

        verify(dialogService).showWarningDialogAndWait("Import warnings", "Check the syntax and encoding of the BibTeX file and correct any errors.\n\ninvalid.bib\n\nSee the event log for details. Some entries may have been imported.");
    }

    @Test
    @ExtendWith(JavaFxExtension.class)
    void partiallyImportedBibFileHasWarningStatus(@TempDir Path directory) throws Exception {
        when(bibDatabaseContext.getDatabasePath()).thenReturn(Optional.empty());
        ImportHandler handler = Mockito.spy(importHandler);
        Mockito.doNothing().when(handler).importEntries(any());
        Path file = directory.resolve("partial.bib");
        Files.writeString(file, """
                @article{valid, title={A valid entry}}
                @article{broken, title={unclosed}
                """);

        List<ImportFilesResultItemViewModel> results = handler.importFilesInBackground(List.of(file), TransferMode.COPY).call();
        JavaFxExtension.awaitEvents();

        assertEquals(1, results.size());
        assertEquals(ImportFilesResultItemViewModel.Status.WARNING, results.getFirst().status());
        assertTrue(results.getFirst().isSuccess());
        assertTrue(results.getFirst().message().get().contains("File was imported as BibTeX"));
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
                new JabRefUndoManager(),
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

        assertEquals(ImportFilesResultItemViewModel.Status.WARNING, outcome.status());
        assertEquals(List.of(importedEntry), outcome.entriesToAdd());
        assertEquals("File was imported as RIS, but warnings were reported: Warning text", outcome.message());
    }

    @Test
    void createAutoDetectionImportOutcomeReturnsEmptyEntryAndWarningMessageWhenNoEntriesWereParsed() {
        ParserResult parserResult = new ParserResult();
        parserResult.addWarning("Warning text");

        ImportResult importResult = new ImportResult("RIS", parserResult);

        ImportHandler.AutoDetectionImportOutcome outcome = importHandler.createAutoDetectionImportOutcome(Path.of("sample.ris"), importResult);

        assertEquals(ImportFilesResultItemViewModel.Status.FAILURE, outcome.status());
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
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.empty());
        ImportHandler handler = new ImportHandler(
                realContext,
                preferences,
                new DummyFileUpdateMonitor(),
                new JabRefUndoManager(),
                stateManager,
                mock(DialogService.class),
                new CurrentThreadTaskExecutor());

        EntryImportHandlerTracker tracker = new EntryImportHandlerTracker(stateManager, realContext, 1);
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
                new JabRefUndoManager(),
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
                new JabRefUndoManager(),
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
                new JabRefUndoManager(),
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
        List<Path> files = List.of(Path.of("paper.pdf"));

        assertTrue(importHandler.confirmBibFileImportIfNecessary(files));

        verifyNoInteractions(dialogService);
    }

    @Test
    void confirmBibFileImportReturnsTrueWhenWarningDisabled() {
        ImporterPreferences importerPreferences = preferences.getImporterPreferences();
        when(importerPreferences.shouldWarnAboutBibFileImport()).thenReturn(false);

        List<Path> files = List.of(Path.of("library.bib"));

        assertTrue(importHandler.confirmBibFileImportIfNecessary(files));

        verifyNoInteractions(dialogService);
    }

    @Test
    void confirmBibFileImportReturnsFalseWhenCancelled() {
        ImporterPreferences importerPreferences = preferences.getImporterPreferences();
        when(importerPreferences.shouldWarnAboutBibFileImport()).thenReturn(true);

        when(dialogService.showConfirmationDialogWithOptOutAndWait(
                any(), any(), any(), any(), any(), any()))
                .thenReturn(false);

        List<Path> files = List.of(Path.of("library.bib"));

        assertFalse(importHandler.confirmBibFileImportIfNecessary(files));

        verify(importerPreferences, never())
                .setWarnAboutBibFileImport(anyBoolean());
    }

    @Test
    void confirmBibFileImportUpdatesPreferenceWhenOptOutSelectedAndConfirmed() {
        ImporterPreferences importerPreferences = preferences.getImporterPreferences();
        when(importerPreferences.shouldWarnAboutBibFileImport()).thenReturn(true);

        doAnswer(invocation -> {
            Consumer<Boolean> optOutConsumer = invocation.getArgument(5);
            optOutConsumer.accept(true);
            return true;
        }).when(dialogService).showConfirmationDialogWithOptOutAndWait(
                any(), any(), any(), any(), any(), any());

        List<Path> files = List.of(Path.of("library.bib"));

        assertTrue(importHandler.confirmBibFileImportIfNecessary(files));

        verify(importerPreferences).setWarnAboutBibFileImport(false);
    }

    @Test
    void confirmBibFileImportUpdatesPreferenceWhenOptOutSelectedAndCancelled() {
        ImporterPreferences importerPreferences = preferences.getImporterPreferences();
        when(importerPreferences.shouldWarnAboutBibFileImport()).thenReturn(true);

        doAnswer(invocation -> {
            Consumer<Boolean> optOutConsumer = invocation.getArgument(5);
            optOutConsumer.accept(true);
            return false;
        }).when(dialogService).showConfirmationDialogWithOptOutAndWait(
                any(), any(), any(), any(), any(), any());

        List<Path> files = List.of(Path.of("library.bib"));

        assertFalse(importHandler.confirmBibFileImportIfNecessary(files));

        verify(importerPreferences).setWarnAboutBibFileImport(false);
    }

    @Test
    void canImportAsBibEntryReturnsTrueForBibFile() {
        assertTrue(importHandler.canImportAsBibEntry(Path.of("test.bib")));
        assertTrue(importHandler.canImportAsBibEntry(Path.of("test.BIB")));
    }

    @Test
    void canImportAsBibEntryReturnsFalseForPdfFile() {
        assertFalse(importHandler.canImportAsBibEntry(Path.of("test.pdf")));
        assertFalse(importHandler.canImportAsBibEntry(Path.of("test.PDF")));
    }

    @Test
    void canImportAsBibEntryReturnsTrueForRisFile() {
        assertTrue(importHandler.canImportAsBibEntry(Path.of("test.ris")));
        assertTrue(importHandler.canImportAsBibEntry(Path.of("test.RIS")));
    }

    @Test
    void canImportAsBibEntryReturnsFalseForGenericExtensions() {
        assertFalse(importHandler.canImportAsBibEntry(Path.of("notes.txt")));
        assertFalse(importHandler.canImportAsBibEntry(Path.of("data.xml")));
        assertFalse(importHandler.canImportAsBibEntry(Path.of("config.yml")));
        assertFalse(importHandler.canImportAsBibEntry(Path.of("config.yaml")));
    }

    @Test
    void canImportAsBibEntryReturnsFalseForUnknownFile() {
        assertFalse(importHandler.canImportAsBibEntry(Path.of("test.unknown")));
    }

    /// Entering a DOI in the New Entry dialog ends here, through
    /// `importEntryWithDuplicateCheck`. The insert has to be a step of its own: undoing it takes
    /// the entry out again, and until then the library says it needs saving.
    @Test
    // [utest->req~logic.undo.entry-insert-recorded~1]
    void importingAnEntryIsOneUndoStep() {
        BibDatabase database = new BibDatabase();
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);
        JabRefUndoManager journal = new JabRefUndoManager();
        ImportHandler handler = handlerFor(databaseContext, journal);

        handler.importCleanedEntries(null, List.of(testEntry));

        assertEquals(List.of(testEntry), database.getEntries());
        assertTrue(journal.hasChanged(), "the library was not reported as needing a save");
        assertTrue(journal.canUndo(), "the import was not recorded");

        journal.undo();

        assertEquals(List.of(), database.getEntries());
        assertFalse(journal.canUndo(), "the import left more than one step behind");
    }

    /// An import that collected nothing — cancelled, or every file failed — is not a step, and must
    /// not report the library as needing a save.
    @Test
    void importingNothingIsNotAnUndoStep() {
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase());
        JabRefUndoManager journal = new JabRefUndoManager();
        ImportHandler handler = handlerFor(databaseContext, journal);

        handler.importCleanedEntries(null, List.of());

        assertFalse(journal.canUndo(), "an empty import became a step");
        assertFalse(journal.hasChanged(), "an empty import reported the library as changed");
    }

    /// Replacing a duplicate removes the entry that was there. Undoing the import must not leave
    /// the library without either entry, so the removal is recorded too.
    @Test
    void replacingADuplicateCanBeUndoneBackToTheOriginal() {
        BibEntry existing = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Existing2023")
                .withField(StandardField.AUTHOR, "Existing Author");
        BibDatabase database = new BibDatabase();
        database.insertEntry(existing);
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);
        JabRefUndoManager journal = new JabRefUndoManager();
        // The decision is asked of the user on the JavaFX thread; this test supplies it instead.
        ImportHandler handler = Mockito.spy(handlerFor(databaseContext, journal));
        Mockito.doReturn(CompletableFuture.completedFuture(
                       new DuplicateDecisionResult(DuplicateResolverDialog.DuplicateResolverResult.KEEP_RIGHT, null)))
               .when(handler).getDuplicateDecision(testEntry, existing, DuplicateResolverDialog.DuplicateResolverResult.BREAK);

        handler.handleDuplicates(testEntry, existing, DuplicateResolverDialog.DuplicateResolverResult.BREAK)
               .thenAccept(entryToImport -> handler.importCleanedEntries(null, List.of(entryToImport.orElseThrow())))
               .join();

        assertEquals(List.of(testEntry), database.getEntries());

        journal.undo();
        assertEquals(List.of(), database.getEntries(), "the import was not taken back");

        journal.undo();
        assertEquals(List.of(existing), database.getEntries(), "the replaced entry could not be restored");
    }

    /// An imported entry may carry a key from its source that is already taken in the library.
    /// With "Generate new key on import" (the default setting), the imported entry gets a free key and the existing key stays.
    @Test
    void importedEntryWithTakenKeyGetsNewKeyAndExistingKeyStays() {
        BibEntry existingWang = new BibEntry()
                .withField(StandardField.AUTHOR, "汪, 小明")
                .withField(StandardField.YEAR, "2020")
                .withCitationKey("Wang2020");
        BibEntry importedWang = new BibEntry()
                .withField(StandardField.AUTHOR, "王, 大明")
                .withField(StandardField.YEAR, "2020")
                .withCitationKey("Wang2020");
        BibDatabase database = new BibDatabase(List.of(existingWang));
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);
        when(preferences.getImporterPreferences().shouldGenerateNewKeyOnImport()).thenReturn(true);
        when(preferences.getMergeDialogPreferences()).thenReturn(mock(MergeDialogPreferences.class));
        GlobalCitationKeyPatterns keyPatterns = GlobalCitationKeyPatterns.fromPattern("[auth:transliterate:camel][year]");
        when(preferences.getCitationKeyPatternPreferences()).thenReturn(new CitationKeyPatternPreferences(
                false,
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A,
                "",
                "",
                CitationKeyPatternPreferences.DEFAULT_UNWANTED_CHARACTERS,
                keyPatterns,
                new SimpleObjectProperty<>(',')));

        handlerFor(databaseContext, new JabRefUndoManager()).importEntries(List.of(importedWang));

        assertEquals(List.of(Optional.of("Wang2020"), Optional.of("Wang2020a")),
                List.of(existingWang.getCitationKey(), importedWang.getCitationKey()));
    }

    @Test
    void importCitedEntriesPreservesExplicitKeysWhenGenerateNewKeyOnImportIsEnabled() {
        BibDatabase database = new BibDatabase();
        BibDatabaseContext databaseContext = new BibDatabaseContext(database);
        configureCitedImportPreferences();

        BibEntry entryWithDocumentKey = new BibEntry(StandardEntryType.Article)
                .withCitationKey("DocumentKey")
                .withField(StandardField.AUTHOR, "Smith, Jane")
                .withField(StandardField.YEAR, "2024");
        BibEntry keylessZoteroEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Doe, John")
                .withField(StandardField.YEAR, "2025");

        handlerFor(databaseContext, new JabRefUndoManager())
                .importEntriesWithDuplicateCheckPreservingCitationKeys(List.of(entryWithDocumentKey, keylessZoteroEntry));

        assertEquals(2, database.getEntries().size(), database.getEntries().toString());
        assertEquals(List.of("DocumentKey", "Doe2025"), database.getEntries().stream()
                                                                .map(entry -> entry.getCitationKey().orElseThrow())
                                                                .toList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Smith2024", "DocumentKey"})
    @ResourceLock("Localization.lang")
    @ExtendWith(JavaFxExtension.class)
        // [utest->req~import.libreoffice.unambiguous-keys~1]
    void importCitedEntriesDeclinesAnExplicitKeyOwnedByAnUnrelatedEarlierImport(String explicitKey) {
        DeferredTaskExecutor executor = new DeferredTaskExecutor();
        configureCitedImportPreferences();
        BibDatabase database = new BibDatabase();
        ImportHandler handler = handlerFor(new BibDatabaseContext(database), new JabRefUndoManager(), executor);
        List<BibEntry> zoteroEntries = CitedEntryCollector.collectCitedEntries(List.of(NATIVE_ZOTERO_REFERENCE_MARK));
        assertEquals(List.of(Optional.empty()), zoteroEntries.stream().map(BibEntry::getCitationKey).toList());

        JavaFxExtension.invokeAndWait(() -> handler.importEntriesWithDuplicateCheckPreservingCitationKeys(zoteroEntries));
        executor.runAll();

        BibEntry importedZoteroEntry = database.getEntries().getFirst();
        assertEquals(Optional.of("Smith2024"), importedZoteroEntry.getCitationKey());
        List<BibEntry> citedEntries = CitedEntryCollector.collectCitedEntries(List.of(citeWithJabRef(smithEntryWithDocumentKey(explicitKey))));
        assertEquals(List.of(Optional.of(explicitKey)), citedEntries.stream().map(BibEntry::getCitationKey).toList());
        assertEquals(Optional.empty(), handler.findDuplicate(citedEntries.getFirst()));

        JavaFxExtension.invokeAndWait(() -> handler.importEntriesWithDuplicateCheckPreservingCitationKeys(citedEntries));
        executor.runAll();

        boolean conflict = "Smith2024".equals(explicitKey);
        assertEquals(conflict ? List.of(importedZoteroEntry) : List.of(importedZoteroEntry, smithEntryWithDocumentKey(explicitKey)), database.getEntries());
        assertEquals(conflict ? List.of(Optional.of("Smith2024")) : List.of(Optional.of("Smith2024"), Optional.of(explicitKey)),
                database.getEntries().stream().map(BibEntry::getCitationKey).toList());
        assertEquals(conflict ? List.of(Optional.of("10.1234/native.2024")) : List.of(Optional.of("10.1234/native.2024"), Optional.of("10.1234/jabref.2024")),
                database.getEntries().stream().map(entry -> entry.getField(StandardField.DOI)).toList());
        if (conflict) {
            verifyCitationKeyWarning(explicitKey);
        } else {
            Mockito.verifyNoInteractions(dialogService);
        }
    }

    @Test
    @ResourceLock("Localization.lang")
    @ExtendWith(JavaFxExtension.class)
        // [utest->req~import.libreoffice.unambiguous-keys~1]
    void importCitedEntriesReservesLaterExplicitKeysBeforeGeneratingKeys() {
        DeferredTaskExecutor executor = new DeferredTaskExecutor();
        configureCitedImportPreferences();
        BibDatabase database = new BibDatabase();
        List<BibEntry> citedEntries = CitedEntryCollector.collectCitedEntries(List.of(
                NATIVE_ZOTERO_REFERENCE_MARK,
                citeWithJabRef(smithEntryWithDocumentKey("Smith2024"))));
        assertEquals(List.of(Optional.empty(), Optional.of("Smith2024")), citedEntries.stream().map(BibEntry::getCitationKey).toList());
        BibEntry expectedZoteroEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Smith2024a")
                .withField(StandardField.AUTHOR, "Smith, Jane")
                .withField(StandardField.DATE, "2024")
                .withField(StandardField.TITLE, "Quantum transport in graphene")
                .withField(StandardField.DOI, "10.1234/native.2024");

        ImportHandler handler = handlerFor(new BibDatabaseContext(database), new JabRefUndoManager(), executor);
        JavaFxExtension.invokeAndWait(() -> handler.importEntriesWithDuplicateCheckPreservingCitationKeys(citedEntries));
        executor.runAll();

        assertEquals(List.of(expectedZoteroEntry, smithEntryWithDocumentKey("Smith2024")), database.getEntries());
        assertEquals(List.of(Optional.of("Smith2024a"), Optional.of("Smith2024")), database.getEntries().stream().map(BibEntry::getCitationKey).toList());
        assertEquals(List.of(Optional.of("10.1234/native.2024"), Optional.of("10.1234/jabref.2024")), database.getEntries().stream().map(entry -> entry.getField(StandardField.DOI)).toList());
        assertEquals(Optional.of("10.1234/jabref.2024"), Citation.lookup(database, "Smith2024").orElseThrow().entry.getField(StandardField.DOI));
        assertEquals(List.of(smithEntryWithDocumentKey("Smith2024")), CitedEntryExporter.export(List.of("Smith2024"), List.of(database)).database().getEntries());
        assertEquals(List.of(Optional.empty(), Optional.of("Smith2024")), citedEntries.stream().map(BibEntry::getCitationKey).toList());
        Mockito.verifyNoInteractions(dialogService);
    }

    @ParameterizedTest
    @EnumSource(value = DuplicateResolverDialog.DuplicateResolverResult.class, names = {"KEEP_LEFT", "KEEP_RIGHT", "KEEP_BOTH", "KEEP_MERGE"})
    @ResourceLock("Localization.lang")
    @ExtendWith(JavaFxExtension.class)
    void importCitedEntriesUsesCompatibleDuplicateDecisionsWhenExplicitKeyAlreadyExists(DuplicateResolverDialog.DuplicateResolverResult decision) {
        DeferredTaskExecutor executor = new DeferredTaskExecutor();
        configureCitedImportPreferences();
        BibEntry existingEntry = smithEntryWithDocumentKey("Smith2024").withField(StandardField.NOTE, "Library notes");
        BibDatabase database = new BibDatabase(List.of(existingEntry));
        ImportHandler handler = Mockito.spy(handlerFor(new BibDatabaseContext(database), new JabRefUndoManager(), executor));
        BibEntry expectedImportedEntry = smithEntryWithDocumentKey("Smith2024").withField(StandardField.NOTE, "Document notes");
        List<BibEntry> citedEntries = CitedEntryCollector.collectCitedEntries(List.of(citeWithJabRef(expectedImportedEntry)));
        assertEquals(List.of(Optional.of("Smith2024")), citedEntries.stream().map(BibEntry::getCitationKey).toList());
        BibEntry citedEntry = citedEntries.getFirst();
        assertEquals(Optional.of(existingEntry), handler.findDuplicate(citedEntry));
        Mockito.doReturn(CompletableFuture.completedFuture(new DuplicateDecisionResult(decision, expectedImportedEntry)))
               .when(handler).getDuplicateDecision(any(BibEntry.class), same(existingEntry), eq(DuplicateResolverDialog.DuplicateResolverResult.BREAK));

        JavaFxExtension.invokeAndWait(() -> handler.importEntriesWithDuplicateCheckPreservingCitationKeys(citedEntries));
        executor.runAll();

        ArgumentCaptor<BibEntry> importedEntryCaptor = ArgumentCaptor.forClass(BibEntry.class);
        Mockito.verify(handler).getDuplicateDecision(importedEntryCaptor.capture(), same(existingEntry), eq(DuplicateResolverDialog.DuplicateResolverResult.BREAK));
        assertNotSame(citedEntry, importedEntryCaptor.getValue());
        assertEquals(expectedImportedEntry, importedEntryCaptor.getValue());
        List<BibEntry> expectedEntries = switch (decision) {
            case KEEP_RIGHT,
                 KEEP_MERGE ->
                    List.of(expectedImportedEntry);
            default ->
                    List.of(existingEntry);
        };
        assertEquals(expectedEntries, database.getEntries());
        assertEquals(List.of(Optional.of("Smith2024")), database.getEntries().stream().map(BibEntry::getCitationKey).toList());
        if (decision == DuplicateResolverDialog.DuplicateResolverResult.KEEP_BOTH) {
            verifyCitationKeyWarning("Smith2024");
        } else {
            Mockito.verifyNoInteractions(dialogService);
        }
    }

    @Test
    @ExtendWith(JavaFxExtension.class)
    @ResourceLock("Localization.lang")
    void importCitedEntriesReservesExplicitSuffixesAsWellAsBaseKeys() {
        configureCitedImportPreferences();
        BibEntry existingEntry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Smith2024")
                .withField(StandardField.TITLE, "Field handbook");
        BibDatabase database = new BibDatabase(List.of(existingEntry));
        DeferredTaskExecutor executor = new DeferredTaskExecutor();
        ImportHandler handler = handlerFor(new BibDatabaseContext(database), new JabRefUndoManager(), executor);
        List<BibEntry> citedEntries = CitedEntryCollector.collectCitedEntries(List.of(
                NATIVE_ZOTERO_REFERENCE_MARK,
                citeWithJabRef(smithEntryWithDocumentKey("Smith2024a"))));

        JavaFxExtension.invokeAndWait(() -> handler.importEntriesWithDuplicateCheckPreservingCitationKeys(citedEntries));
        executor.runAll();

        assertEquals(List.of(Optional.of("Smith2024"), Optional.of("Smith2024b"), Optional.of("Smith2024a")),
                database.getEntries().stream().map(BibEntry::getCitationKey).toList());
        assertEquals(Optional.of("10.1234/native.2024"), database.getEntryByCitationKey("Smith2024b").orElseThrow().getField(StandardField.DOI));
        assertEquals(Optional.of("10.1234/jabref.2024"), database.getEntryByCitationKey("Smith2024a").orElseThrow().getField(StandardField.DOI));
        Mockito.verifyNoInteractions(dialogService);
    }

    @ParameterizedTest
    @EnumSource(value = DuplicateResolverDialog.DuplicateResolverResult.class, names = {"KEEP_LEFT", "KEEP_RIGHT", "KEEP_BOTH", "KEEP_MERGE", "BREAK"})
    @ExtendWith(JavaFxExtension.class)
    @ResourceLock("Localization.lang")
        // [utest->req~import.libreoffice.batch-duplicates~1]
    void citedBatchWaitsForInsertionAndDuplicateDecisionBeforeContinuing(DuplicateResolverDialog.DuplicateResolverResult decision) {
        configureCitedImportPreferences();
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase());
        DeferredTaskExecutor executor = new DeferredTaskExecutor();
        ImportHandler handler = Mockito.spy(handlerFor(context, new JabRefUndoManager(), executor));
        List<BibEntry> citedEntries = distinctZoteroItemsForSamePaper();
        assertEquals(2, citedEntries.size());
        assertEquals(List.of(Optional.empty(), Optional.empty()), citedEntries.stream().map(BibEntry::getCitationKey).toList());
        CompletableFuture<DuplicateDecisionResult> duplicateDecision = new CompletableFuture<>();
        Mockito.doReturn(duplicateDecision).when(handler).getDuplicateDecision(any(), any(), eq(DuplicateResolverDialog.DuplicateResolverResult.BREAK));
        EntryImportHandlerTracker tracker = trackerFor(context, citedEntries.size());
        AtomicInteger finished = new AtomicInteger();
        tracker.setOnFinish(finished::incrementAndGet);
        CompletableFuture<Void> completed = startCitedImport(handler, citedEntries, tracker);

        assertEquals(1, executor.pendingCount());
        executor.runNext();
        assertEquals(List.of(), context.getEntries());
        assertEquals(1, executor.pendingCount());
        executor.runNext();
        assertEquals(1, tracker.getImportedCount());
        assertEquals(1, executor.pendingCount());
        executor.runNext();
        assertEquals(0, executor.pendingCount());
        assertFalse(completed.isDone());
        assertEquals(0, finished.get());
        verify(handler).getDuplicateDecision(any(), same(context.getEntries().getFirst()), eq(DuplicateResolverDialog.DuplicateResolverResult.BREAK));

        JavaFxExtension.invokeAndWait(() -> duplicateDecision.complete(new DuplicateDecisionResult(decision, new BibEntry(citedEntries.getLast()))));
        executor.runAll();

        int expectedEntryCount = decision == DuplicateResolverDialog.DuplicateResolverResult.KEEP_BOTH ? 2 : 1;
        assertEquals(expectedEntryCount, context.getEntries().size());
        assertEquals(expectedEntryCount == 2 ? List.of(Optional.of("Smith2024"), Optional.of("Smith2024a")) : List.of(Optional.of("Smith2024")),
                context.getEntries().stream().map(BibEntry::getCitationKey).toList());
        assertEquals(expectedEntryCount == 2 ? List.of(Optional.of("10.1234/native.2024"), Optional.of("10.1234/native.2024")) : List.of(Optional.of("10.1234/native.2024")),
                context.getEntries().stream().map(entry -> entry.getField(StandardField.DOI)).toList());
        boolean skippedDuplicate = decision == DuplicateResolverDialog.DuplicateResolverResult.KEEP_LEFT || decision == DuplicateResolverDialog.DuplicateResolverResult.BREAK;
        assertEquals(skippedDuplicate ? 1 : 2, tracker.getImportedCount());
        assertEquals(skippedDuplicate ? 1 : 0, tracker.getSkippedCount());
        assertEquals(decision == DuplicateResolverDialog.DuplicateResolverResult.KEEP_LEFT ? context.getEntries() : List.of(), tracker.getKeptExistingEntries());
        assertEquals(1, finished.get());
        assertTrue(completed.isDone());
        assertFalse(completed.isCompletedExceptionally());
    }

    @ParameterizedTest
    @EnumSource(value = DuplicateResolverDialog.DuplicateResolverResult.class, names = {"KEEP_LEFT", "KEEP_RIGHT", "KEEP_BOTH", "KEEP_MERGE"})
    @ExtendWith(JavaFxExtension.class)
    @ResourceLock("Localization.lang")
    void citedDuplicateDecisionsMustHonorTheDocumentKey(DuplicateResolverDialog.DuplicateResolverResult decision) {
        configureCitedImportPreferences();
        BibEntry existingEntry = smithEntryWithDocumentKey("LibraryKey");
        BibEntry documentEntry = smithEntryWithDocumentKey("Smith2024");
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(existingEntry)));
        JabRefUndoManager journal = new JabRefUndoManager();
        DeferredTaskExecutor executor = new DeferredTaskExecutor();
        ImportHandler handler = Mockito.spy(handlerFor(context, journal, executor));
        Mockito.doReturn(CompletableFuture.completedFuture(new DuplicateDecisionResult(decision, new BibEntry(existingEntry))))
               .when(handler).getDuplicateDecision(any(), same(existingEntry), eq(DuplicateResolverDialog.DuplicateResolverResult.BREAK));
        EntryImportHandlerTracker tracker = trackerFor(context, 1);

        CompletableFuture<Void> completed = startCitedImport(handler, List.of(documentEntry), tracker);
        executor.runAll();

        List<BibEntry> expected = switch (decision) {
            case KEEP_RIGHT ->
                    List.of(documentEntry);
            case KEEP_BOTH ->
                    List.of(existingEntry, documentEntry);
            default ->
                    List.of(existingEntry);
        };
        assertEquals(expected, context.getEntries());
        assertEquals(expected.stream().map(BibEntry::getCitationKey).toList(), context.getEntries().stream().map(BibEntry::getCitationKey).toList());
        assertEquals(expected.stream().map(entry -> entry.getField(StandardField.DOI)).toList(), context.getEntries().stream().map(entry -> entry.getField(StandardField.DOI)).toList());
        if (decision == DuplicateResolverDialog.DuplicateResolverResult.KEEP_LEFT || decision == DuplicateResolverDialog.DuplicateResolverResult.KEEP_MERGE) {
            verifyCitationKeyWarning("Smith2024");
            assertEquals(1, tracker.getSkippedCount());
            assertEquals(List.of(), tracker.getKeptExistingEntries());
            assertFalse(journal.canUndo());
        } else {
            assertEquals(1, tracker.getImportedCount());
            Mockito.verifyNoInteractions(dialogService);
        }
        assertTrue(completed.isDone());
        assertFalse(completed.isCompletedExceptionally());
    }

    @ParameterizedTest
    @EnumSource(value = DuplicateResolverDialog.DuplicateResolverResult.class, names = {"KEEP_RIGHT", "KEEP_MERGE"})
    @ExtendWith(JavaFxExtension.class)
    @ResourceLock("Localization.lang")
    void citedKeyConflictIsCheckedBeforeRemovingTheBibliographicDuplicate(DuplicateResolverDialog.DuplicateResolverResult decision) {
        configureCitedImportPreferences();
        BibEntry duplicate = smithEntryWithDocumentKey("LibraryKey");
        BibEntry keyOwner = new BibEntry(CitedEntryCollector.collectCitedEntries(List.of(NATIVE_ZOTERO_REFERENCE_MARK)).getFirst()).withCitationKey("Smith2024");
        BibEntry documentEntry = smithEntryWithDocumentKey("Smith2024");
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(duplicate, keyOwner)));
        JabRefUndoManager journal = new JabRefUndoManager();
        DeferredTaskExecutor executor = new DeferredTaskExecutor();
        ImportHandler handler = Mockito.spy(handlerFor(context, journal, executor));
        Mockito.doReturn(CompletableFuture.completedFuture(new DuplicateDecisionResult(decision, documentEntry)))
               .when(handler).getDuplicateDecision(any(), same(duplicate), eq(DuplicateResolverDialog.DuplicateResolverResult.BREAK));
        EntryImportHandlerTracker tracker = trackerFor(context, 1);

        CompletableFuture<Void> completed = startCitedImport(handler, List.of(documentEntry), tracker);
        executor.runAll();

        assertEquals(List.of(duplicate, keyOwner), context.getEntries());
        assertEquals(List.of(Optional.of("LibraryKey"), Optional.of("Smith2024")), context.getEntries().stream().map(BibEntry::getCitationKey).toList());
        assertEquals(List.of(Optional.of("10.1234/jabref.2024"), Optional.of("10.1234/native.2024")), context.getEntries().stream().map(entry -> entry.getField(StandardField.DOI)).toList());
        assertEquals(1, tracker.getSkippedCount());
        assertFalse(journal.canUndo());
        assertTrue(completed.isDone());
        verifyCitationKeyWarning("Smith2024");
    }

    @ParameterizedTest
    @CsvSource({"KEEP_RIGHT,keyless", "KEEP_MERGE,keyless", "KEEP_RIGHT,Smith2024B", "KEEP_MERGE,Smith2024B"})
    @ExtendWith(JavaFxExtension.class)
    @ResourceLock("Localization.lang")
        // [utest->req~import.libreoffice.unambiguous-keys~1]
    void citedReplacementCannotDisplaceAnEarlierExplicitBatchKey(DuplicateResolverDialog.DuplicateResolverResult decision, String replacementKey) {
        configureCitedImportPreferences();
        BibEntry firstEntry = CitedEntryCollector.collectCitedEntries(List.of(NATIVE_ZOTERO_REFERENCE_MARK)).getFirst().withCitationKey("Smith2024");
        String laterMark = "keyless".equals(replacementKey)
                           ? NATIVE_ZOTERO_REFERENCE_MARK.replace("587", "588")
                                                         .replace("users/123/items/ABCD1234", "users/456/items/WXYZ9876")
                                                         .replace("RNDabcdefghij", "RNDklmnopqrst")
                           : citeWithJabRef(new BibEntry(firstEntry).withCitationKey(replacementKey), Map.of(replacementKey, "http://zotero.org/users/456/items/WXYZ9876"));
        List<BibEntry> entries = CitedEntryCollector.collectCitedEntries(List.of(
                citeWithJabRef(firstEntry, Map.of("Smith2024", "http://zotero.org/users/123/items/ABCD1234")),
                laterMark,
                citeWithJabRef(smithEntryWithDocumentKey("DocumentKey"))));
        assertEquals(List.of(Optional.of("Smith2024"), "keyless".equals(replacementKey) ? Optional.empty() : Optional.of(replacementKey), Optional.of("DocumentKey")),
                entries.stream().map(BibEntry::getCitationKey).toList());
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase());
        DeferredTaskExecutor executor = new DeferredTaskExecutor();
        ImportHandler handler = Mockito.spy(handlerFor(context, new JabRefUndoManager(), executor));
        CompletableFuture<DuplicateDecisionResult> duplicateDecision = new CompletableFuture<>();
        Mockito.doReturn(duplicateDecision).when(handler).getDuplicateDecision(any(), any(), eq(DuplicateResolverDialog.DuplicateResolverResult.BREAK));
        EntryImportHandlerTracker tracker = trackerFor(context, entries.size());
        AtomicInteger finished = new AtomicInteger();
        tracker.setOnFinish(finished::incrementAndGet);
        CompletableFuture<Void> completed = startCitedImport(handler, entries, tracker);
        executor.runNext();
        executor.runNext();
        BibEntry insertedFirstEntry = context.getDatabase().getEntryByCitationKey("Smith2024").orElseThrow();
        executor.runNext();
        verify(handler).getDuplicateDecision(any(), same(insertedFirstEntry), eq(DuplicateResolverDialog.DuplicateResolverResult.BREAK));
        assertFalse(completed.isDone());

        JavaFxExtension.invokeAndWait(() -> duplicateDecision.complete(new DuplicateDecisionResult(decision, new BibEntry(entries.get(1)))));
        assertSame(insertedFirstEntry, context.getDatabase().getEntryByCitationKey("Smith2024").orElseThrow());
        executor.runAll();

        BibEntry expectedFirstEntry = new BibEntry(firstEntry).withField(StandardField.DATE, "2024");
        expectedFirstEntry.clearField(StandardField.YEAR);
        assertEquals(List.of(expectedFirstEntry, smithEntryWithDocumentKey("DocumentKey")), context.getEntries());
        assertSame(insertedFirstEntry, context.getDatabase().getEntryByCitationKey("Smith2024").orElseThrow());
        assertEquals(List.of(Optional.of("Smith2024"), Optional.of("DocumentKey")), context.getEntries().stream().map(BibEntry::getCitationKey).toList());
        assertEquals(List.of(Optional.of("10.1234/native.2024"), Optional.of("10.1234/jabref.2024")), context.getEntries().stream().map(entry -> entry.getField(StandardField.DOI)).toList());
        assertEquals(2, tracker.getImportedCount());
        assertEquals(1, tracker.getSkippedCount());
        assertEquals(1, finished.get());
        assertTrue(completed.isDone());
        assertFalse(completed.isCompletedExceptionally());
        verifyCitationKeyWarning("Smith2024");
    }

    @ParameterizedTest
    @EnumSource(value = DuplicateResolverDialog.DuplicateResolverResult.class, names = {"KEEP_RIGHT", "KEEP_MERGE"})
    @ExtendWith(JavaFxExtension.class)
    @ResourceLock("Localization.lang")
    void keylessCitedReplacementCanDisplaceAnExistingKeyNotReservedByTheBatch(DuplicateResolverDialog.DuplicateResolverResult decision) {
        configureCitedImportPreferences();
        BibEntry existingEntry = CitedEntryCollector.collectCitedEntries(List.of(NATIVE_ZOTERO_REFERENCE_MARK)).getFirst().withCitationKey("LibraryKey");
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(existingEntry)));
        List<BibEntry> entries = CitedEntryCollector.collectCitedEntries(List.of(
                NATIVE_ZOTERO_REFERENCE_MARK,
                citeWithJabRef(smithEntryWithDocumentKey("DocumentKey"))));
        DeferredTaskExecutor executor = new DeferredTaskExecutor();
        ImportHandler handler = Mockito.spy(handlerFor(context, new JabRefUndoManager(), executor));
        Mockito.doReturn(CompletableFuture.completedFuture(new DuplicateDecisionResult(decision, new BibEntry(entries.getFirst()))))
               .when(handler).getDuplicateDecision(any(), same(existingEntry), eq(DuplicateResolverDialog.DuplicateResolverResult.BREAK));
        EntryImportHandlerTracker tracker = trackerFor(context, entries.size());

        CompletableFuture<Void> completed = startCitedImport(handler, entries, tracker);
        executor.runAll();

        assertEquals(List.of(Optional.of("Smith2024"), Optional.of("DocumentKey")), context.getEntries().stream().map(BibEntry::getCitationKey).toList());
        assertEquals(List.of(Optional.of("10.1234/native.2024"), Optional.of("10.1234/jabref.2024")), context.getEntries().stream().map(entry -> entry.getField(StandardField.DOI)).toList());
        assertEquals(List.of(), context.getDatabase().getEntriesByCitationKey("LibraryKey"));
        assertEquals(2, tracker.getImportedCount());
        assertEquals(0, tracker.getSkippedCount());
        assertTrue(completed.isDone());
        assertFalse(completed.isCompletedExceptionally());
        Mockito.verifyNoInteractions(dialogService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @ExtendWith(JavaFxExtension.class)
    @ResourceLock("Localization.lang")
    void cancellingCitedSearchOrLinkedFileAdjustmentContinuesTheBatch(boolean cancelAdjustment) {
        configureCitedImportPreferences();
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase());
        DeferredTaskExecutor executor = new DeferredTaskExecutor();
        ImportHandler handler = handlerFor(context, new JabRefUndoManager(), executor);
        List<BibEntry> entries = CitedEntryCollector.collectCitedEntries(List.of(NATIVE_ZOTERO_REFERENCE_MARK, citeWithJabRef(smithEntryWithDocumentKey("DocumentKey"))));
        EntryImportHandlerTracker tracker = trackerFor(context, entries.size());
        CompletableFuture<Void> completed = startCitedImport(handler, entries, tracker);
        if (cancelAdjustment) {
            executor.runNext();
        }

        executor.cancelNext();
        executor.runAll();

        assertEquals(List.of(smithEntryWithDocumentKey("DocumentKey")), context.getEntries());
        assertEquals(List.of(Optional.of("DocumentKey")), context.getEntries().stream().map(BibEntry::getCitationKey).toList());
        assertEquals(1, tracker.getImportedCount());
        assertEquals(1, tracker.getSkippedCount());
        assertTrue(completed.isDone());
        assertFalse(completed.isCompletedExceptionally());
    }

    @ParameterizedTest
    @ValueSource(strings = {"cancelled", "failed", "closed"})
    @ExtendWith(JavaFxExtension.class)
    @ResourceLock("Localization.lang")
    void interruptedOrClosedCitedDuplicateDecisionContinuesTheBatch(String outcome) {
        configureCitedImportPreferences();
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase());
        DeferredTaskExecutor executor = new DeferredTaskExecutor();
        ImportHandler handler = Mockito.spy(handlerFor(context, new JabRefUndoManager(), executor));
        List<BibEntry> entries = new ArrayList<>(distinctZoteroItemsForSamePaper());
        entries.add(smithEntryWithDocumentKey("DocumentKey"));
        CompletableFuture<DuplicateDecisionResult> decision = new CompletableFuture<>();
        Mockito.doReturn(decision).when(handler).getDuplicateDecision(any(), any(), eq(DuplicateResolverDialog.DuplicateResolverResult.BREAK));
        EntryImportHandlerTracker tracker = trackerFor(context, entries.size());
        CompletableFuture<Void> completed = startCitedImport(handler, entries, tracker);
        executor.runAll();
        assertFalse(completed.isDone());
        assertEquals(1, context.getEntries().size());

        JavaFxExtension.invokeAndWait(() -> {
            switch (outcome) {
                case "cancelled" ->
                        decision.cancel(false);
                case "closed" ->
                        decision.complete(new DuplicateDecisionResult(DuplicateResolverDialog.DuplicateResolverResult.BREAK, entries.get(1)));
                default ->
                        decision.completeExceptionally(new IOException("Cannot resolve duplicate"));
            }
        });
        executor.runAll();

        assertEquals(List.of(Optional.of("Smith2024"), Optional.of("DocumentKey")), context.getEntries().stream().map(BibEntry::getCitationKey).toList());
        assertEquals(List.of(Optional.of("10.1234/native.2024"), Optional.of("10.1234/jabref.2024")), context.getEntries().stream().map(entry -> entry.getField(StandardField.DOI)).toList());
        assertEquals(2, tracker.getImportedCount());
        assertEquals(1, tracker.getSkippedCount());
        assertTrue(completed.isDone());
        assertFalse(completed.isCompletedExceptionally());
    }

    @Test
    @ExtendWith(JavaFxExtension.class)
    @ResourceLock("Localization.lang")
    void failedCitedDuplicateSearchContinuesTheBatch() {
        configureCitedImportPreferences();
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase());
        DeferredTaskExecutor executor = new DeferredTaskExecutor();
        ImportHandler handler = Mockito.spy(handlerFor(context, new JabRefUndoManager(), executor));
        Mockito.doThrow(new IllegalArgumentException("Cannot search duplicates")).doCallRealMethod().when(handler).findDuplicate(any());
        List<BibEntry> entries = CitedEntryCollector.collectCitedEntries(List.of(NATIVE_ZOTERO_REFERENCE_MARK, citeWithJabRef(smithEntryWithDocumentKey("DocumentKey"))));
        EntryImportHandlerTracker tracker = trackerFor(context, entries.size());

        CompletableFuture<Void> completed = startCitedImport(handler, entries, tracker);
        executor.runAll();

        assertEquals(List.of(smithEntryWithDocumentKey("DocumentKey")), context.getEntries());
        assertEquals(1, tracker.getImportedCount());
        assertEquals(1, tracker.getSkippedCount());
        assertTrue(completed.isDone());
        assertFalse(completed.isCompletedExceptionally());
    }

    @Test
    @ExtendWith(JavaFxExtension.class)
    @ResourceLock("Localization.lang")
    void citedBatchReadsApplyToAllAfterTheEarlierDuplicateDecision() {
        configureCitedImportPreferences();
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase());
        DeferredTaskExecutor executor = new DeferredTaskExecutor();
        ImportHandler handler = Mockito.spy(handlerFor(context, new JabRefUndoManager(), executor));
        List<BibEntry> entries = new ArrayList<>(distinctZoteroItemsForSamePaper());
        entries.add(new BibEntry(entries.getFirst()));
        Mockito.doAnswer(_ -> {
            when(preferences.getMergeDialogPreferences().shouldMergeApplyToAllEntries()).thenReturn(true);
            when(preferences.getMergeDialogPreferences().getAllEntriesDuplicateResolverDecision()).thenReturn(DuplicateResolverDialog.DuplicateResolverResult.KEEP_LEFT);
            return CompletableFuture.completedFuture(new DuplicateDecisionResult(DuplicateResolverDialog.DuplicateResolverResult.KEEP_LEFT, entries.getFirst()));
        }).when(handler).getDuplicateDecision(any(), any(), eq(DuplicateResolverDialog.DuplicateResolverResult.BREAK));
        Mockito.doReturn(CompletableFuture.completedFuture(new DuplicateDecisionResult(DuplicateResolverDialog.DuplicateResolverResult.KEEP_LEFT, entries.getFirst())))
               .when(handler).getDuplicateDecision(any(), any(), eq(DuplicateResolverDialog.DuplicateResolverResult.KEEP_LEFT));
        EntryImportHandlerTracker tracker = trackerFor(context, entries.size());

        CompletableFuture<Void> completed = startCitedImport(handler, entries, tracker);
        executor.runAll();

        verify(handler).getDuplicateDecision(any(), any(), eq(DuplicateResolverDialog.DuplicateResolverResult.BREAK));
        verify(handler).getDuplicateDecision(any(), any(), eq(DuplicateResolverDialog.DuplicateResolverResult.KEEP_LEFT));
        assertEquals(List.of(Optional.of("Smith2024")), context.getEntries().stream().map(BibEntry::getCitationKey).toList());
        assertEquals(1, tracker.getImportedCount());
        assertEquals(2, tracker.getSkippedCount());
        assertTrue(completed.isDone());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @ExtendWith(JavaFxExtension.class)
    @ResourceLock("Localization.lang")
    void ordinaryBatchImportsRemainConcurrentAndRespectKeyRegeneration(boolean generateNewKeys) {
        configureCitedImportPreferences();
        when(preferences.getImporterPreferences().shouldGenerateNewKeyOnImport()).thenReturn(generateNewKeys);
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase());
        DeferredTaskExecutor executor = new DeferredTaskExecutor();
        ImportHandler handler = handlerFor(context, new JabRefUndoManager(), executor);
        List<BibEntry> entries = CitedEntryCollector.collectCitedEntries(List.of(NATIVE_ZOTERO_REFERENCE_MARK, citeWithJabRef(smithEntryWithDocumentKey("DocumentKey"))));
        entries.getFirst().withCitationKey("OriginalKey");

        JavaFxExtension.invokeAndWait(() -> handler.importEntriesWithDuplicateCheck(null, entries));
        assertEquals(2, executor.pendingCount());
        executor.runAll();

        assertEquals(generateNewKeys ? List.of(Optional.of("Smith2024"), Optional.of("Smith2024a")) : List.of(Optional.of("OriginalKey"), Optional.of("DocumentKey")),
                context.getEntries().stream().map(BibEntry::getCitationKey).toList());
        assertEquals(List.of(Optional.of("10.1234/native.2024"), Optional.of("10.1234/jabref.2024")), context.getEntries().stream().map(entry -> entry.getField(StandardField.DOI)).toList());
        assertEquals(List.of(Optional.of("OriginalKey"), Optional.of("DocumentKey")), entries.stream().map(BibEntry::getCitationKey).toList());
        assertEquals(generateNewKeys, preferences.getImporterPreferences().shouldGenerateNewKeyOnImport());
    }

    private static List<BibEntry> distinctZoteroItemsForSamePaper() {
        return CitedEntryCollector.collectCitedEntries(List.of(
                NATIVE_ZOTERO_REFERENCE_MARK,
                NATIVE_ZOTERO_REFERENCE_MARK.replace("587", "588")
                                            .replace("users/123/items/ABCD1234", "users/456/items/WXYZ9876")
                                            .replace("RNDabcdefghij", "RNDklmnopqrst")));
    }

    private EntryImportHandlerTracker trackerFor(BibDatabaseContext context, int totalEntries) {
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.empty());
        return new EntryImportHandlerTracker(stateManager, context, totalEntries);
    }

    private static CompletableFuture<Void> startCitedImport(ImportHandler handler, List<BibEntry> entries, EntryImportHandlerTracker tracker) {
        AtomicReference<CompletableFuture<Void>> result = new AtomicReference<>();
        JavaFxExtension.invokeAndWait(() -> result.set(handler.importEntriesWithDuplicateCheckPreservingCitationKeys(entries, tracker)));
        return result.get();
    }

    private void verifyCitationKeyWarning(String key) {
        verify(dialogService).showWarningDialogAndWait(
                Localization.lang("Unable to import cited entries"),
                Localization.lang("The cited entry with citation key %0 was not imported because the key could not be preserved. Import into an empty library or resolve the citation key conflict before trying again. The document was not changed.", key));
    }

    /// Keeps work separate from JavaFX callbacks, so queued searches cannot accidentally observe inline insertions.
    @NullMarked
    private static class DeferredTaskExecutor extends CurrentThreadTaskExecutor {
        private final Deque<PendingTask<?>> tasks = new ArrayDeque<>();

        @Override
        public <V> Future<V> execute(BackgroundTask<V> task) {
            FutureTask<V> work = new FutureTask<>(task::call);
            tasks.addLast(new PendingTask<>(task, work));
            return work;
        }

        int pendingCount() {
            return tasks.size();
        }

        void runNext() {
            assertFalse(Platform.isFxApplicationThread());
            tasks.removeFirst().run();
        }

        void cancelNext() {
            PendingTask<?> pending = tasks.removeFirst();
            JavaFxExtension.invokeAndWait(pending.task()::cancel);
            pending.work().cancel(false);
        }

        void runAll() {
            for (int processed = 0; !tasks.isEmpty(); processed++) {
                assertTrue(processed < 100, "Import tasks did not finish");
                runNext();
            }
        }
    }

    @NullMarked
    private record PendingTask<V>(BackgroundTask<V> task, FutureTask<V> work) {
        void run() {
            work.run();
            if (work.state() == Future.State.FAILED) {
                Exception failure = assertInstanceOf(Exception.class, work.exceptionNow());
                JavaFxExtension.invokeAndWait(() -> task.getOnException().accept(failure));
            } else {
                V result = work.resultNow();
                JavaFxExtension.invokeAndWait(() -> task.getOnSuccess().accept(result));
            }
        }
    }

    private static BibEntry smithEntryWithDocumentKey(String citationKey) {
        return new BibEntry(StandardEntryType.Article)
                .withCitationKey(citationKey)
                .withField(StandardField.AUTHOR, "Smith, Jane")
                .withField(StandardField.DATE, "2024")
                .withField(StandardField.TITLE, "Medieval trade across the Baltic")
                .withField(StandardField.DOI, "10.1234/jabref.2024");
    }

    private static String citeWithJabRef(BibEntry entry) {
        return citeWithJabRef(entry, Map.of());
    }

    private static String citeWithJabRef(BibEntry entry, Map<String, String> zoteroUris) {
        return ZoteroReferenceMark.buildReferenceMark(
                List.of(entry),
                List.of(entry.getCitationKey().orElseThrow()),
                1,
                CitationType.AUTHORYEAR_PAR,
                new BibDatabaseContext(new BibDatabase(List.of(entry))),
                new BibEntryTypesManager(),
                zoteroUris).name();
    }

    private void configureCitedImportPreferences() {
        when(preferences.getImporterPreferences().shouldGenerateNewKeyOnImport()).thenReturn(true);
        when(preferences.getMergeDialogPreferences()).thenReturn(mock(MergeDialogPreferences.class));
        GlobalCitationKeyPatterns keyPatterns = GlobalCitationKeyPatterns.fromPattern("[auth:transliterate:camel][year]");
        when(preferences.getCitationKeyPatternPreferences()).thenReturn(new CitationKeyPatternPreferences(
                false,
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A,
                "",
                "",
                CitationKeyPatternPreferences.DEFAULT_UNWANTED_CHARACTERS,
                keyPatterns,
                new SimpleObjectProperty<>(',')));
    }

    private ImportHandler handlerFor(BibDatabaseContext databaseContext, JabRefUndoManager journal) {
        return handlerFor(databaseContext, journal, new CurrentThreadTaskExecutor());
    }

    private ImportHandler handlerFor(BibDatabaseContext databaseContext, JabRefUndoManager journal, TaskExecutor executor) {
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedGroups(any())).thenReturn(FXCollections.observableArrayList());
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.empty());
        return new ImportHandler(
                databaseContext,
                preferences,
                new DummyFileUpdateMonitor(),
                journal,
                stateManager,
                dialogService,
                executor);
    }
}
