package org.jabref.gui.fieldeditors;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.EmptySuggestionProvider;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkedFilesEditorViewModelTest {
    private LinkedFilesEditorViewModel ViewModel;

    private DialogService dialogService;

    private ImportFormatPreferences importFormatPreferences;

    private ImporterPreferences importerPreferences;


    @BeforeEach
    void setUp(){
        // code taken from org.jabref.logic.importer.WebFetchersTest.setUp
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        importerPreferences = mock(ImporterPreferences.class);
        FieldContentFormatterPreferences fieldContentFormatterPreferences = mock(FieldContentFormatterPreferences.class);
        when(importFormatPreferences.getFieldContentFormatterPreferences()).thenReturn(fieldContentFormatterPreferences);
    }

    @Test
    void emptyUrlNotext(){
        ViewModel = new LinkedFilesEditorViewModel(StandardField.FILE, new EmptySuggestionProvider(), mock(DialogService.class), mock(BibDatabaseContext.class),
                new CurrentThreadTaskExecutor(), mock(FieldCheckers.class), mock(PreferencesService.class, Answers.RETURNS_DEEP_STUBS));
        ViewModel.entry = new BibEntry();
        ViewModel.entry.setField(StandardField.URL, "http://example.com");
        dialogService = mock(DialogService.class);
        ViewModel.fetchFulltext();
    }
}
