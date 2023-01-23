package org.jabref.gui.fieldeditors;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.EmptySuggestionProvider;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.mockito.Mockito.mock;

class LinkedFilesEditorViewModelTest {

    private LinkedFilesEditorViewModel viewModel;

    private DialogService dialogService;

    @Test
    void emptyUrlNotext() {
        viewModel = new LinkedFilesEditorViewModel(StandardField.FILE, new EmptySuggestionProvider(), mock(DialogService.class), mock(BibDatabaseContext.class),
                           new CurrentThreadTaskExecutor(), mock(FieldCheckers.class), mock(PreferencesService.class, Answers.RETURNS_DEEP_STUBS));
        viewModel.entry = new BibEntry();
        viewModel.entry.setField(StandardField.URL, "http://example.com");
        dialogService = mock(DialogService.class);
        viewModel.fetchFulltext();
    }
}
