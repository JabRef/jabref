package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.entryeditor.citationrelationtab.semanticscholar.CitationFetcher;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.database.DuplicateCheck;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

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
    private PreferencesService preferencesService;
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
        when(preferencesService.getImportFormatPreferences()).thenReturn(importFormatPreferences);

        ImporterPreferences importerPreferences = mock(ImporterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importerPreferences.isGenerateNewKeyOnImport()).thenReturn(false);
        when(preferencesService.getImporterPreferences()).thenReturn(importerPreferences);

        when(preferencesService.getFilePreferences()).thenReturn(mock(FilePreferences.class));
        when(preferencesService.getOwnerPreferences()).thenReturn(mock(OwnerPreferences.class, Answers.RETURNS_DEEP_STUBS));
        when(preferencesService.getTimestampPreferences()).thenReturn(mock(TimestampPreferences.class, Answers.RETURNS_DEEP_STUBS));

        CitationKeyPatternPreferences citationKeyPatternPreferences = mock(CitationKeyPatternPreferences.class);
        GlobalCitationKeyPattern pattern = GlobalCitationKeyPattern.fromPattern("[auth][year]");
        when(citationKeyPatternPreferences.getKeyPattern()).thenReturn(pattern);
        when(preferencesService.getCitationKeyPatternPreferences()).thenReturn(citationKeyPatternPreferences);

        bibDatabaseContext = new BibDatabaseContext(new BibDatabase());
        when(duplicateCheck.isDuplicate(any(), any(), any())).thenReturn(false);

        viewModel = new CitationsRelationsTabViewModel(
                bibDatabaseContext,
                preferencesService,
                mock(UndoManager.class),
                mock(StateManager.class, Answers.RETURNS_DEEP_STUBS),
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
    void testExistingEntryCitesOtherPaperWithCitationKeys() {
        var citationItems = List.of(new CitationRelationItem(firstEntryToImport, false),
                new CitationRelationItem(secondEntryToImport, false));

        viewModel.importEntries(citationItems, CitationFetcher.SearchType.CITES, existingEntry);
        assertEquals(Optional.of("FirstAuthorCitationKey2022,SecondAuthorCitationKey20221"), existingEntry.getField(StandardField.CITES));
        assertEquals(List.of(existingEntry, firstEntryToImport, secondEntryToImport), bibDatabaseContext.getEntries());
    }

    @Test
    void testImportedEntriesWithExistingCitationKeysCiteExistingEntry() {
        var citationItems = List.of(new CitationRelationItem(firstEntryToImport, false),
                new CitationRelationItem(secondEntryToImport, false));

        viewModel.importEntries(citationItems, CitationFetcher.SearchType.CITED_BY, existingEntry);
        assertEquals(Optional.of("Test2023"), firstEntryToImport.getField(StandardField.CITES));
        assertEquals(List.of(existingEntry, firstEntryToImport, secondEntryToImport), bibDatabaseContext.getEntries());
    }

    @Test
    void testExistingEntryCitesOtherPaperWithCitationKeysAndExistingCiteField() {
        existingEntry.setField(StandardField.CITES, "Asdf1222");
        var citationItems = List.of(new CitationRelationItem(firstEntryToImport, false),
                new CitationRelationItem(secondEntryToImport, false));

        viewModel.importEntries(citationItems, CitationFetcher.SearchType.CITES, existingEntry);
        assertEquals(Optional.of("Asdf1222,FirstAuthorCitationKey2022,SecondAuthorCitationKey20221"), existingEntry.getField(StandardField.CITES));
        assertEquals(List.of(existingEntry, firstEntryToImport, secondEntryToImport), bibDatabaseContext.getEntries());
    }
}
