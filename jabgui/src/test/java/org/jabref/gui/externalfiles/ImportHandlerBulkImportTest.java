package org.jabref.gui.externalfiles;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.undo.UndoManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.LibraryPreferences;
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
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportHandlerBulkImportTest {

    @Mock
    private GuiPreferences preferences;

    @Mock
    private DialogService dialogService;

    private StateManager stateManager;
    private BibDatabaseContext databaseContext;
    private BibDatabase database;
    private ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        database = new BibDatabase();
        databaseContext = mock(BibDatabaseContext.class);
        when(databaseContext.getMode()).thenReturn(BibDatabaseMode.BIBTEX);
        when(databaseContext.getDatabase()).thenReturn(database);

        stateManager = mock(StateManager.class);
        ObservableList<GroupTreeNode> noGroups = FXCollections.observableArrayList();
        when(stateManager.getSelectedGroups(any())).thenReturn(noGroups);

        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getImportFormatPreferences()).thenReturn(importFormatPreferences);
        when(preferences.getExternalApplicationsPreferences()).thenReturn(
                new ExternalApplicationsPreferences("", false, Set.of(), false, "", false, "", ""));
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

        LibraryPreferences libraryPreferences = new LibraryPreferences(BibDatabaseMode.BIBTEX, false, false, false, "");
        when(preferences.getLibraryPreferences()).thenReturn(libraryPreferences);

        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.observableArrayList());
        when(preferences.getFieldPreferences()).thenReturn(fieldPreferences);
    }

    @Test
    void singleEntryDoesNotTriggerBulkCallbacks() {
        AtomicInteger startCalls = new AtomicInteger();
        AtomicInteger endCalls = new AtomicInteger();
        ImportHandler handler = newHandler(startCalls::incrementAndGet, endCalls::incrementAndGet, databaseContext);

        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "One");

        handler.importCleanedEntries(null, List.of(entry));

        assertEquals(0, startCalls.get(), "Start callback should not run for single entry");
        assertEquals(0, endCalls.get(), "End callback should not run for single entry");
        assertTrue(database.getEntries().contains(entry), "Entry should be stored");
    }

    @Test
    void multipleEntriesTriggerBulkCallbacksOnce() {
        AtomicInteger startCalls = new AtomicInteger();
        AtomicInteger endCalls = new AtomicInteger();
        ImportHandler handler = newHandler(startCalls::incrementAndGet, endCalls::incrementAndGet, databaseContext);

        BibEntry first = new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "First");
        BibEntry second = new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "Second");

        handler.importCleanedEntries(null, List.of(first, second));

        assertEquals(1, startCalls.get(), "Start callback should run once");
        assertEquals(1, endCalls.get(), "End callback should run once");
        assertTrue(database.getEntries().containsAll(List.of(first, second)), "All entries should be stored");
    }

    @Test
    void endCallbackRunsEvenWhenInsertFails() {
        AtomicInteger startCalls = new AtomicInteger();
        AtomicInteger endCalls = new AtomicInteger();

        BibDatabase failingDatabase = mock(BibDatabase.class);
        doThrow(new RuntimeException("insert failed")).when(failingDatabase).insertEntries(anyList());

        BibDatabaseContext failingContext = mock(BibDatabaseContext.class);
        when(failingContext.getMode()).thenReturn(BibDatabaseMode.BIBTEX);
        when(failingContext.getDatabase()).thenReturn(failingDatabase);

        ImportHandler handler = newHandler(startCalls::incrementAndGet, endCalls::incrementAndGet, failingContext);

        BibEntry entry = new BibEntry(StandardEntryType.Article).withField(StandardField.TITLE, "Broken");

        try {
            handler.importCleanedEntries(null, List.of(entry));
            fail("Expected RuntimeException from failing insert");
        } catch (RuntimeException expected) {
            // expected
        }

        assertEquals(1, startCalls.get(), "Start callback should run once");
        assertEquals(1, endCalls.get(), "End callback should run once even on failure");
    }

    private ImportHandler newHandler(Runnable start, Runnable end, BibDatabaseContext context) {
        return new ImportHandler(
                context,
                preferences,
                new DummyFileUpdateMonitor(),
                mock(UndoManager.class),
                stateManager,
                dialogService,
                new CurrentThreadTaskExecutor(),
                start,
                end);
    }
}
