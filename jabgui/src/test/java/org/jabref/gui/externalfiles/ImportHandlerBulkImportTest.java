package org.jabref.gui.externalfiles;

import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
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
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportHandlerBulkImportTest {

    private ImportHandler importHandler;
    private BibDatabaseContext bibDatabaseContext;
    private BibDatabase bibDatabase;
    private Runnable onBulkImportStart;
    private Runnable onBulkImportEnd;
    private int startCallCount;
    private int endCallCount;

    @Mock
    private GuiPreferences preferences;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        startCallCount = 0;
        endCallCount = 0;

        onBulkImportStart = () -> startCallCount++;
        onBulkImportEnd = () -> endCallCount++;

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getImportFormatPreferences()).thenReturn(importFormatPreferences);
        when(preferences.getFilePreferences()).thenReturn(mock(FilePreferences.class));
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
        when(importerPreferences.shouldGenerateNewKeyOnImport()).thenReturn(false);
        when(preferences.getImporterPreferences()).thenReturn(importerPreferences);
        OwnerPreferences ownerPreferences = mock(OwnerPreferences.class);
        when(ownerPreferences.isUseOwner()).thenReturn(false);
        when(ownerPreferences.getDefaultOwner()).thenReturn("");
        when(ownerPreferences.isOverwriteOwner()).thenReturn(false);
        when(preferences.getOwnerPreferences()).thenReturn(ownerPreferences);
        TimestampPreferences timestampPreferences = mock(TimestampPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(timestampPreferences.shouldAddCreationDate()).thenReturn(false);
        when(timestampPreferences.now()).thenReturn("");
        when(preferences.getTimestampPreferences()).thenReturn(timestampPreferences);

        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.observableArrayList());
        when(preferences.getFieldPreferences()).thenReturn(fieldPreferences);

        bibDatabase = new BibDatabase();
        bibDatabaseContext = mock(BibDatabaseContext.class);
        when(bibDatabaseContext.getMode()).thenReturn(BibDatabaseMode.BIBTEX);
        when(bibDatabaseContext.getDatabase()).thenReturn(bibDatabase);

        importHandler = new ImportHandler(
                bibDatabaseContext,
                preferences,
                new DummyFileUpdateMonitor(),
                mock(UndoManager.class),
                mock(StateManager.class),
                mock(DialogService.class),
                new CurrentThreadTaskExecutor(),
                onBulkImportStart,
                onBulkImportEnd);
    }

    @Test
    void importCleanedEntriesWithSingleEntryDoesNotCallBulkImportCallbacks() {
        // Arrange
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Single2023")
                .withField(StandardField.AUTHOR, "Single Author");
        List<BibEntry> entries = List.of(entry);

        // Act
        importHandler.importCleanedEntries(null, entries);

        // Assert
        assertEquals(0, startCallCount, "Bulk import start should not be called for single entry");
        assertEquals(0, endCallCount, "Bulk import end should not be called for single entry");
        assertTrue(bibDatabase.getEntries().contains(entry), "Entry should be added to database");
    }

    @Test
    void importCleanedEntriesWithMultipleEntriesCallsBulkImportCallbacks() {
        // Arrange
        BibEntry entry1 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Bulk1")
                .withField(StandardField.AUTHOR, "Author 1");
        BibEntry entry2 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Bulk2")
                .withField(StandardField.AUTHOR, "Author 2");
        BibEntry entry3 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Bulk3")
                .withField(StandardField.AUTHOR, "Author 3");
        List<BibEntry> entries = List.of(entry1, entry2, entry3);

        // Act
        importHandler.importCleanedEntries(null, entries);

        // Assert
        assertEquals(1, startCallCount, "Bulk import start should be called once");
        assertEquals(1, endCallCount, "Bulk import end should be called once");
        assertTrue(bibDatabase.getEntries().contains(entry1), "Entry 1 should be added to database");
        assertTrue(bibDatabase.getEntries().contains(entry2), "Entry 2 should be added to database");
        assertTrue(bibDatabase.getEntries().contains(entry3), "Entry 3 should be added to database");
    }

    @Test
    void importCleanedEntriesCallsEndCallbackEvenOnException() {
        // Arrange
        BibEntry entry1 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Exception1")
                .withField(StandardField.AUTHOR, "Author 1");
        BibEntry entry2 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Exception2")
                .withField(StandardField.AUTHOR, "Author 2");
        List<BibEntry> entries = List.of(entry1, entry2);

        // Mock to throw exception
        when(bibDatabaseContext.getDatabase()).thenThrow(new RuntimeException("Test exception"));

        // Act & Assert
        try {
            importHandler.importCleanedEntries(null, entries);
        } catch (RuntimeException e) {
            // Expected
        }

        // Assert that end callback was still called
        assertEquals(1, startCallCount, "Bulk import start should be called");
        assertEquals(1, endCallCount, "Bulk import end should be called even on exception");
    }

    @Test
    void importHandlerWithNullCallbacksWorksCorrectly() {
        // Arrange
        ImportHandler handlerWithNullCallbacks = new ImportHandler(
                bibDatabaseContext,
                preferences,
                new DummyFileUpdateMonitor(),
                mock(UndoManager.class),
                mock(StateManager.class),
                mock(DialogService.class),
                new CurrentThreadTaskExecutor(),
                null,
                null);

        BibEntry entry1 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Null1")
                .withField(StandardField.AUTHOR, "Author 1");
        BibEntry entry2 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Null2")
                .withField(StandardField.AUTHOR, "Author 2");
        List<BibEntry> entries = List.of(entry1, entry2);

        // Act
        handlerWithNullCallbacks.importCleanedEntries(null, entries);

        // Assert - should not throw NullPointerException
        assertTrue(bibDatabase.getEntries().contains(entry1), "Entry 1 should be added");
        assertTrue(bibDatabase.getEntries().contains(entry2), "Entry 2 should be added");
    }
}
