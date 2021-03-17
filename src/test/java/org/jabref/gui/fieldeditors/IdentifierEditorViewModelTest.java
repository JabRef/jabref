package org.jabref.gui.fieldeditors;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.EmptySuggestionProvider;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class IdentifierEditorViewModelTest {

    private IdentifierEditorViewModel viewModel;

    @BeforeEach
    void setUp() throws Exception {
        viewModel = new IdentifierEditorViewModel(StandardField.DOI, new EmptySuggestionProvider(), new CurrentThreadTaskExecutor(), mock(DialogService.class), mock(FieldCheckers.class), mock(PreferencesService.class));
    }

    @Test
    void validIdentifierIsNotPresentIsTrueForEmptyText() throws Exception {
        assertTrue(viewModel.validIdentifierIsNotPresentProperty().get());
    }
}
