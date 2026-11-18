package org.jabref.gui.externalfiles;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.mergeentries.MergeDialogPreferences;
import org.jabref.gui.preferences.GuiPreferences;
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
import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.logic.openoffice.action.ImportCited;
import org.jabref.logic.openoffice.oocsltext.CSLCitationType;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.undo.JabRefUndoManager;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
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
    void importCitedEntriesPreservesExplicitKeyAfterEarlierKeylessZoteroImport(String explicitKey) {
        configureCitedImportPreferences();
        BibDatabase database = new BibDatabase();
        ImportHandler handler = handlerFor(new BibDatabaseContext(database), new JabRefUndoManager());
        List<BibEntry> zoteroEntries = ImportCited.collectCitedEntries(List.of(NATIVE_ZOTERO_REFERENCE_MARK));
        assertEquals(List.of(Optional.empty()), zoteroEntries.stream().map(BibEntry::getCitationKey).toList());

        handler.importEntriesWithDuplicateCheckPreservingCitationKeys(zoteroEntries);

        BibEntry importedZoteroEntry = database.getEntries().getFirst();
        assertEquals(Optional.of("Smith2024"), importedZoteroEntry.getCitationKey());
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(List.of(citeWithJabRef(smithEntryWithDocumentKey(explicitKey))));
        assertEquals(List.of(Optional.of(explicitKey)), citedEntries.stream().map(BibEntry::getCitationKey).toList());
        assertEquals(Optional.empty(), handler.findDuplicate(citedEntries.getFirst()));

        handler.importEntriesWithDuplicateCheckPreservingCitationKeys(citedEntries);

        assertEquals(List.of(importedZoteroEntry, smithEntryWithDocumentKey(explicitKey)), database.getEntries());
        assertEquals(List.of(Optional.of("Smith2024"), Optional.of(explicitKey)),
                database.getEntries().stream().map(BibEntry::getCitationKey).toList());
        assertEquals(List.of(Optional.of("10.1234/native.2024"), Optional.of("10.1234/jabref.2024")),
                database.getEntries().stream().map(entry -> entry.getField(StandardField.DOI)).toList());
    }

    @Test
    @ResourceLock("Localization.lang")
    void importCitedEntriesKeepsUnrelatedPublicationsWhenGeneratedKeyMatchesLaterExplicitKeyInSameBatch() {
        configureCitedImportPreferences();
        BibDatabase database = new BibDatabase();
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(List.of(
                NATIVE_ZOTERO_REFERENCE_MARK,
                citeWithJabRef(smithEntryWithDocumentKey("Smith2024"))));
        assertEquals(List.of(Optional.empty(), Optional.of("Smith2024")), citedEntries.stream().map(BibEntry::getCitationKey).toList());
        BibEntry expectedZoteroEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Smith2024")
                .withField(StandardField.AUTHOR, "Smith, Jane")
                .withField(StandardField.DATE, "2024")
                .withField(StandardField.TITLE, "Quantum transport in graphene")
                .withField(StandardField.DOI, "10.1234/native.2024");

        handlerFor(new BibDatabaseContext(database), new JabRefUndoManager())
                .importEntriesWithDuplicateCheckPreservingCitationKeys(citedEntries);

        assertEquals(List.of(expectedZoteroEntry, smithEntryWithDocumentKey("Smith2024")), database.getEntries());
    }

    @ParameterizedTest
    @EnumSource(value = DuplicateResolverDialog.DuplicateResolverResult.class, names = {"KEEP_LEFT", "KEEP_RIGHT", "KEEP_BOTH"})
    @ResourceLock("Localization.lang")
    void importCitedEntriesUsesOrdinaryDuplicateDecisionWhenExplicitKeyAlreadyExists(DuplicateResolverDialog.DuplicateResolverResult decision) {
        configureCitedImportPreferences();
        BibEntry existingEntry = smithEntryWithDocumentKey("Smith2024").withField(StandardField.NOTE, "Library notes");
        BibDatabase database = new BibDatabase(List.of(existingEntry));
        ImportHandler handler = Mockito.spy(handlerFor(new BibDatabaseContext(database), new JabRefUndoManager()));
        BibEntry expectedImportedEntry = smithEntryWithDocumentKey("Smith2024").withField(StandardField.NOTE, "Document notes");
        List<BibEntry> citedEntries = ImportCited.collectCitedEntries(List.of(citeWithJabRef(expectedImportedEntry)));
        assertEquals(List.of(Optional.of("Smith2024")), citedEntries.stream().map(BibEntry::getCitationKey).toList());
        BibEntry citedEntry = citedEntries.getFirst();
        assertEquals(Optional.of(existingEntry), handler.findDuplicate(citedEntry));
        Mockito.doReturn(CompletableFuture.completedFuture(new DuplicateDecisionResult(decision, expectedImportedEntry)))
               .when(handler).getDuplicateDecision(any(BibEntry.class), same(existingEntry), eq(DuplicateResolverDialog.DuplicateResolverResult.BREAK));

        handler.importEntriesWithDuplicateCheckPreservingCitationKeys(citedEntries);

        ArgumentCaptor<BibEntry> importedEntryCaptor = ArgumentCaptor.forClass(BibEntry.class);
        Mockito.verify(handler).getDuplicateDecision(importedEntryCaptor.capture(), same(existingEntry), eq(DuplicateResolverDialog.DuplicateResolverResult.BREAK));
        assertNotSame(citedEntry, importedEntryCaptor.getValue());
        assertEquals(expectedImportedEntry, importedEntryCaptor.getValue());
        List<BibEntry> expectedEntries = switch (decision) {
            case KEEP_RIGHT ->
                    List.of(expectedImportedEntry);
            case KEEP_BOTH ->
                    List.of(existingEntry, expectedImportedEntry);
            default ->
                    List.of(existingEntry);
        };
        assertEquals(expectedEntries, database.getEntries());
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
        return ZoteroReferenceMark.buildReferenceMark(
                List.of(entry),
                List.of(entry.getCitationKey().orElseThrow()),
                List.of(0),
                1,
                CSLCitationType.NORMAL,
                new BibDatabaseContext(new BibDatabase(List.of(entry))),
                new BibEntryTypesManager(),
                Map.of()).getName();
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
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getSelectedGroups(any())).thenReturn(FXCollections.observableArrayList());
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.empty());
        return new ImportHandler(
                databaseContext,
                preferences,
                new DummyFileUpdateMonitor(),
                journal,
                stateManager,
                mock(DialogService.class),
                new CurrentThreadTaskExecutor());
    }
}
