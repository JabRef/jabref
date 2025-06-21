package org.jabref.gui.mergebibfilesintocurrentbib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.mergeentries.MergeEntriesAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockitoAnnotations;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
public class MergeBibFilesIntoCurrentBibTest {
    @TempDir Path tempDir;

    private Path testFolder;
    private Path testInnerFolder;
    private Path currentDbFile;
    private BibEntry expectedEntry1;

    @Mock
    private DialogService dialogService;
    @Mock
    private GuiPreferences preferences;
    @Mock
    private StateManager stateManager;
    @Mock
    private UndoManager undoManager;
    @Mock
    private FileUpdateMonitor fileUpdateMonitor;
    @Mock
    private BibEntryTypesManager bibEntryTypesManager;
    @Mock
    private MergeBibFilesIntoCurrentBibPreferences mergeBibFilesIntoCurrentBibPreferences;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);

        testFolder = tempDir.resolve("test");
        Files.createDirectory(testFolder);
        testInnerFolder = testFolder.resolve("inner");
        Files.createDirectory(testInnerFolder);
        Path backupDirectory = tempDir.resolve("backups");
        Files.createDirectory(backupDirectory);

        Path bibFile1 = testInnerFolder.resolve("library1.bib");

        currentDbFile = testFolder.resolve("current.bib");

        String bibContent1 = """
                @article{test1,
                  author = {Foo Bar},
                  title = {First Article},
                  journal = {International Journal of Something},
                  year = {2023}
                }""";

        Files.writeString(bibFile1, bibContent1);

        expectedEntry1 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test1")
                .withField(StandardField.AUTHOR, "Foo Bar")
                .withField(StandardField.TITLE, "First Article")
                .withField(StandardField.JOURNAL, "International Journal of Something")
                .withField(StandardField.YEAR, "2023");

        FilePreferences filePreferences = mock(FilePreferences.class);
        when(filePreferences.getWorkingDirectory()).thenReturn(testFolder);
        when(filePreferences.getBackupDirectory()).thenReturn(backupDirectory);

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(preferences.getImportFormatPreferences()).thenReturn(importFormatPreferences);

        when(stateManager.activeDatabaseProperty()).thenReturn(OptionalObjectProperty.empty());

        when(mergeBibFilesIntoCurrentBibPreferences.shouldMergeSameKeyEntries()).thenReturn(true);
        when(mergeBibFilesIntoCurrentBibPreferences.shouldMergeDuplicateEntries()).thenReturn(true);
        when(preferences.getMergeBibFilesIntoCurrentBibPreferences()).thenReturn(mergeBibFilesIntoCurrentBibPreferences);
    }

    @Test
    public void simpleMergeTest() {
        BibDatabase currentDatabase = new BibDatabase();
        BibDatabaseContext currentContext = new BibDatabaseContext(currentDatabase);
        currentContext.setDatabasePath(currentDbFile);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(currentContext));
        when(dialogService.showDirectorySelectionDialog(any())).thenReturn(Optional.of(testInnerFolder));

        MergeBibFilesIntoCurrentBibAction action = new MergeBibFilesIntoCurrentBibAction(
                dialogService,
                preferences,
                stateManager,
                undoManager,
                fileUpdateMonitor,
                bibEntryTypesManager
        );

        action.execute();

        List<BibEntry> entries = new ArrayList<>(currentDatabase.getEntries());
        assertEquals(1, entries.size(), "Should have merged 1 entry");

        BibEntry entry1 = entries.stream()
                                 .filter(e -> "test1".equals(e.getCitationKey().orElse("")))
                                 .findFirst()
                                 .orElseThrow(() -> new AssertionError("Entry 'test1' not found"));

        assertEquals(entry1, expectedEntry1, "test1 does not match after merge");
    }

    @Test
    public void equalEntriesMergeTest() {
        BibDatabase currentDatabase = new BibDatabase();
        currentDatabase.insertEntry(expectedEntry1);
        BibDatabaseContext currentContext = new BibDatabaseContext(currentDatabase);
        currentContext.setDatabasePath(currentDbFile);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(currentContext));
        when(dialogService.showDirectorySelectionDialog(any())).thenReturn(Optional.of(testInnerFolder));

        MergeBibFilesIntoCurrentBibAction action = new MergeBibFilesIntoCurrentBibAction(
                dialogService,
                preferences,
                stateManager,
                undoManager,
                fileUpdateMonitor,
                bibEntryTypesManager
        );

        action.execute();

        List<BibEntry> entries = new ArrayList<>(currentDatabase.getEntries());
        assertEquals(1, entries.size(), "Should not have merged any entry");

        BibEntry entry1 = entries.stream()
                                 .filter(e -> "test1".equals(e.getCitationKey().orElse("")))
                                 .findFirst()
                                 .orElseThrow(() -> new AssertionError("Entry 'test1' not found"));

        assertEquals(entry1, expectedEntry1, "test1 does not match after merge");
    }

    @Test
    public void sameCitationKeyMergeTest() {
        BibEntry currentEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test1")
                .withField(StandardField.AUTHOR, "Different Author")
                .withField(StandardField.TITLE, "Different Title")
                .withField(StandardField.JOURNAL, "Different Journal")
                .withField(StandardField.YEAR, "2025");

        BibDatabase currentDatabase = new BibDatabase();
        currentDatabase.insertEntry(currentEntry);
        BibDatabaseContext currentContext = new BibDatabaseContext(currentDatabase);
        currentContext.setDatabasePath(currentDbFile);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(currentContext));
        when(dialogService.showDirectorySelectionDialog(any())).thenReturn(Optional.of(testInnerFolder));

        try (MockedConstruction<MergeEntriesAction> mockedMergeEntriesAction = mockConstruction(MergeEntriesAction.class,
                (mock, _) -> doNothing().when(mock).execute())) {
            MergeBibFilesIntoCurrentBibAction action = new MergeBibFilesIntoCurrentBibAction(
                    dialogService,
                    preferences,
                    stateManager,
                    undoManager,
                    fileUpdateMonitor,
                    bibEntryTypesManager
            );

            action.execute();
            assertEquals(1, mockedMergeEntriesAction.constructed().size(), "Expected MergeEntriesAction instance not found");
        }
    }

    @Test
    public void sameCitationKeyNoMergePreferenceTest() {
        when(mergeBibFilesIntoCurrentBibPreferences.shouldMergeSameKeyEntries()).thenReturn(false);
        when(mergeBibFilesIntoCurrentBibPreferences.shouldMergeDuplicateEntries()).thenReturn(false);
        BibEntry currentEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test1")
                .withField(StandardField.AUTHOR, "Different Author")
                .withField(StandardField.TITLE, "Different Title")
                .withField(StandardField.JOURNAL, "Different Journal")
                .withField(StandardField.YEAR, "2025");

        BibDatabase currentDatabase = new BibDatabase();
        currentDatabase.insertEntry(currentEntry);
        BibDatabaseContext currentContext = new BibDatabaseContext(currentDatabase);
        currentContext.setDatabasePath(currentDbFile);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(currentContext));
        when(dialogService.showDirectorySelectionDialog(any())).thenReturn(Optional.of(testInnerFolder));

        MergeBibFilesIntoCurrentBibAction action = new MergeBibFilesIntoCurrentBibAction(
                dialogService,
                preferences,
                stateManager,
                undoManager,
                fileUpdateMonitor,
                bibEntryTypesManager
        );

        action.execute();

        List<BibEntry> entries = new ArrayList<>(currentDatabase.getEntries());
        assertEquals(1, entries.size(), "Should still have one entry");

        BibEntry entry1 = entries.stream()
                                 .filter(e -> "test1".equals(e.getCitationKey().orElse("")))
                                 .findFirst()
                                 .orElseThrow(() -> new AssertionError("Entry 'test1' not found"));

        assertEquals(entry1, currentEntry, "test1 does not match after merge");
    }

    @Test
    public void duplicateMergeTest() {
        BibEntry currentEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("DIFFERENTCITATIONKEY")
                .withField(StandardField.AUTHOR, "Foo Bar")
                .withField(StandardField.TITLE, "First Article")
                .withField(StandardField.JOURNAL, "International Journal of Something")
                .withField(StandardField.YEAR, "2023");

        BibDatabase currentDatabase = new BibDatabase();
        currentDatabase.insertEntry(currentEntry);
        BibDatabaseContext currentContext = new BibDatabaseContext(currentDatabase);
        currentContext.setDatabasePath(currentDbFile);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(currentContext));
        when(dialogService.showDirectorySelectionDialog(any())).thenReturn(Optional.of(testInnerFolder));

        try (MockedConstruction<MergeEntriesAction> mockedMergeEntriesAction = mockConstruction(MergeEntriesAction.class,
                (mock, _) -> doNothing().when(mock).execute())) {
            MergeBibFilesIntoCurrentBibAction action = new MergeBibFilesIntoCurrentBibAction(
                    dialogService,
                    preferences,
                    stateManager,
                    undoManager,
                    fileUpdateMonitor,
                    bibEntryTypesManager
            );

            action.execute();
            assertEquals(1, mockedMergeEntriesAction.constructed().size(), "Expected MergeEntriesAction instance not found");
        }
    }

    @Test
    public void duplicateNoMergePreferenceTest() {
        when(mergeBibFilesIntoCurrentBibPreferences.shouldMergeSameKeyEntries()).thenReturn(false);
        when(mergeBibFilesIntoCurrentBibPreferences.shouldMergeDuplicateEntries()).thenReturn(false);
        BibEntry currentEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("DIFFERENTCITATIONKEY")
                .withField(StandardField.AUTHOR, "Foo Bar")
                .withField(StandardField.TITLE, "First Article")
                .withField(StandardField.JOURNAL, "International Journal of Something")
                .withField(StandardField.YEAR, "2023");

        BibDatabase currentDatabase = new BibDatabase();
        currentDatabase.insertEntry(currentEntry);
        BibDatabaseContext currentContext = new BibDatabaseContext(currentDatabase);
        currentContext.setDatabasePath(currentDbFile);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(currentContext));
        when(dialogService.showDirectorySelectionDialog(any())).thenReturn(Optional.of(testInnerFolder));

        MergeBibFilesIntoCurrentBibAction action = new MergeBibFilesIntoCurrentBibAction(
                dialogService,
                preferences,
                stateManager,
                undoManager,
                fileUpdateMonitor,
                bibEntryTypesManager
        );

        action.execute();

        List<BibEntry> entries = new ArrayList<>(currentDatabase.getEntries());
        assertEquals(1, entries.size(), "Should still have one entry");

        BibEntry entry1 = entries.stream()
                                 .filter(e -> "DIFFERENTCITATIONKEY".equals(e.getCitationKey().orElse("")))
                                 .findFirst()
                                 .orElseThrow(() -> new AssertionError("Entry 'test1' not found"));

        assertEquals(entry1, currentEntry, "test1 does not match after merge");
    }

    @Test
    public void multipleDirectoryMergeTest() throws IOException {
        Path bibFile2 = testFolder.resolve("library2.bib");
        Path bibFile3 = testFolder.resolve("library3.bib");

        String bibContent2 = """
                @article{test2,
                  author = {BlaBla},
                  title = {Second Article},
                  journal = {International Journal of Nothing},
                  year = {2024}
                }""";

        String bibContent3 = """
                @article{test3,
                  author = {Foo Bla},
                  title = {Third Article},
                  journal = {International Journal of Anything},
                  year = {2025}
                }""";

        Files.writeString(bibFile2, bibContent2);
        Files.writeString(bibFile3, bibContent3);

        BibEntry expectedEntry2 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test2")
                .withField(StandardField.AUTHOR, "BlaBla")
                .withField(StandardField.TITLE, "Second Article")
                .withField(StandardField.JOURNAL, "International Journal of Nothing")
                .withField(StandardField.YEAR, "2024");

        BibEntry expectedEntry3 = new BibEntry(StandardEntryType.Article)
                .withCitationKey("test3")
                .withField(StandardField.AUTHOR, "Foo Bla")
                .withField(StandardField.TITLE, "Third Article")
                .withField(StandardField.JOURNAL, "International Journal of Anything")
                .withField(StandardField.YEAR, "2025");

        BibDatabase currentDatabase = new BibDatabase();
        BibDatabaseContext currentContext = new BibDatabaseContext(currentDatabase);
        currentContext.setDatabasePath(currentDbFile);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(currentContext));
        when(dialogService.showDirectorySelectionDialog(any())).thenReturn(Optional.of(testFolder));

        MergeBibFilesIntoCurrentBibAction action = new MergeBibFilesIntoCurrentBibAction(
                dialogService,
                preferences,
                stateManager,
                undoManager,
                fileUpdateMonitor,
                bibEntryTypesManager
        );

        action.execute();

        List<BibEntry> entries = new ArrayList<>(currentDatabase.getEntries());
        assertEquals(3, entries.size(), "Should have merged three entries");

        BibEntry entry1 = entries.stream()
                                 .filter(e -> "test1".equals(e.getCitationKey().orElse("")))
                                 .findFirst()
                                 .orElseThrow(() -> new AssertionError("Entry 'test1' not found"));

        assertEquals(entry1, expectedEntry1, "test1 does not match after merge");

        BibEntry entry2 = entries.stream()
                                 .filter(e -> "test2".equals(e.getCitationKey().orElse("")))
                                 .findFirst()
                                 .orElseThrow(() -> new AssertionError("Entry 'test2' not found"));

        assertEquals(entry2, expectedEntry2, "test2 does not match after merge");

        BibEntry entry3 = entries.stream()
                                 .filter(e -> "test3".equals(e.getCitationKey().orElse("")))
                                 .findFirst()
                                 .orElseThrow(() -> new AssertionError("Entry 'test3' not found"));

        assertEquals(entry3, expectedEntry3, "test3 does not match after merge");
    }
}
