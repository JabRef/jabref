package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.citationrelationtab.semanticscholar.CitationFetcher;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.database.DuplicateCheck;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CitationsRelationsTabViewModelTest {
    private ImportHandler importHandler;
    private BibDatabaseContext bibDatabaseContext;
    private BibEntry testEntry;

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

        bibDatabaseContext = new BibDatabaseContext(new BibDatabase());
        bibDatabaseContext.setMode(BibDatabaseMode.BIBTEX);
        when(duplicateCheck.isDuplicate(any(), any(), any())).thenReturn(false);

        StateManager stateManager = mock(StateManager.class, Answers.RETURNS_DEEP_STUBS);

        viewModel = new CitationsRelationsTabViewModel(
                bibDatabaseContext,
                preferences,
                mock(UndoManager.class),
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
        var citationItems = List.of(
                new CitationRelationItem(firstEntryToImport, false),
                new CitationRelationItem(secondEntryToImport, false));

        viewModel.importEntries(citationItems, CitationFetcher.SearchType.CITES, existingEntry);

        assertEquals(Optional.of("FirstAuthorCitationKey2022,SecondAuthorCitationKey20221"), existingEntry.getField(StandardField.CITES));
        assertEquals(List.of(existingEntry, firstEntryToImport, secondEntryToImport), bibDatabaseContext.getEntries());
    }

    @Test
    void importedEntriesWithExistingCitationKeysCiteExistingEntry() {
        var citationItems = List.of(
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
        var citationItems = List.of(
                new CitationRelationItem(firstEntryToImport, false),
                new CitationRelationItem(secondEntryToImport, false));

        viewModel.importEntries(citationItems, CitationFetcher.SearchType.CITES, existingEntry);

        assertEquals(Optional.of("Asdf1222,FirstAuthorCitationKey2022,SecondAuthorCitationKey20221"), existingEntry.getField(StandardField.CITES));
        assertEquals(List.of(existingEntry, firstEntryToImport, secondEntryToImport), bibDatabaseContext.getEntries());
    }
}
