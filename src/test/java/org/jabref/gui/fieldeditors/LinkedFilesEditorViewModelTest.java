package org.jabref.gui.fieldeditors;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.EmptySuggestionProvider;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.mockito.Mockito.mock;

class LinkedFilesEditorViewModelTest {
    private LinkedFilesEditorViewModel ViewModel;

    private DialogService dialogService;

    private ImportFormatPreferences importFormatPreferences;

    private ImporterPreferences importerPreferences;


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
