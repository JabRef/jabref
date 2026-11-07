package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.List;
import java.util.Optional;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citation.SearchCitationsRelationsService;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.citation.CitationFetcherType;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import io.github.kusoroadeolu.veneer.BibTeXSyntaxHighlighter;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
@ExtendWith(JavaFxExtension.class)
class CitationRelationsTabTest {

    private GuiPreferences preferences;
    private StateManager stateManager;
    private SearchCitationsRelationsService searchService;
    private TaskExecutor taskExecutor;
    private CitationRelationsTab tab;
    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() throws FetcherException {
        preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        stateManager = mock(StateManager.class, Answers.RETURNS_DEEP_STUBS);
        searchService = mock(SearchCitationsRelationsService.class);
        taskExecutor = new CurrentThreadTaskExecutor();

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
        when(citationKeyPatternPreferences.getKeyPatterns()).thenReturn(GlobalCitationKeyPatterns.fromPattern("[auth][year]"));
        when(preferences.getCitationKeyPatternPreferences()).thenReturn(citationKeyPatternPreferences);

        LibraryPreferences libraryPreferences = mock(LibraryPreferences.class);
        when(libraryPreferences.shouldAddImportedEntries()).thenReturn(false);
        when(preferences.getLibraryPreferences()).thenReturn(libraryPreferences);

        EntryEditorPreferences entryEditorPreferences = mock(EntryEditorPreferences.class);
        when(entryEditorPreferences.citationFetcherTypeProperty()).thenReturn(new SimpleObjectProperty<>(CitationFetcherType.SEMANTIC_SCHOLAR));
        when(preferences.getEntryEditorPreferences()).thenReturn(entryEditorPreferences);

        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.empty());
        databaseContext = new BibDatabaseContext(new BibDatabase());
        databaseContext.setMode(BibDatabaseMode.BIBTEX);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));

        when(searchService.searchCites(any(), anyBoolean(), any())).thenReturn(List.of());
        when(searchService.searchCitedBy(any(), anyBoolean(), any())).thenReturn(List.of());

        Injector.setModelOrService(ClipBoardManager.class, mock(ClipBoardManager.class));

        JavaFxExtension.invokeAndWait(() -> {
            tab = new CitationRelationsTab(
                    mock(DialogService.class),
                    stateManager,
                    new DummyFileUpdateMonitor(),
                    preferences,
                    taskExecutor,
                    new BibEntryTypesManager(),
                    searchService,
                    mock(BibTeXSyntaxHighlighter.class)
            );
        });
    }

    @Test
    void switchingToTabRefreshesWhenDoiChanged() throws FetcherException {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.DOI, "10.1145/782941.782993");

        JavaFxExtension.invokeAndWait(() -> {
            tab.currentEntryProperty().set(entry);
            tab.notifyAboutFocus(entry);
        });

        verify(searchService, times(1)).searchCites(any(), anyBoolean(), any());

        // Same DOI on focus should not trigger another search
        JavaFxExtension.invokeAndWait(() -> tab.notifyAboutFocus(entry));
        verify(searchService, times(1)).searchCites(any(), anyBoolean(), any());

        // Update DOI on the same entry and focus again
        entry.setField(StandardField.DOI, "10.1038/35057062");
        JavaFxExtension.invokeAndWait(() -> tab.notifyAboutFocus(entry));
        verify(searchService, times(2)).searchCites(any(), anyBoolean(), any());

        // Clearing DOI should not trigger citation search
        entry.clearField(StandardField.DOI);
        JavaFxExtension.invokeAndWait(() -> tab.notifyAboutFocus(entry));
        verify(searchService, times(2)).searchCites(any(), anyBoolean(), any());
    }
}
