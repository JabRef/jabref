package org.jabref.gui.fieldeditors;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.WordSuggestionProvider;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class IdentifierEditorViewModelTest {

    private IdentifierEditorViewModel viewModel;

    @BeforeEach
    public void setUp() throws Exception {
        viewModel = new IdentifierEditorViewModel(StandardField.DOI, new WordSuggestionProvider(StandardField.DOI), new CurrentThreadTaskExecutor(), mock(DialogService.class), mock(FieldCheckers.class));
    }

    @Test
    public void validIdentifierIsNotPresentIsTrueForEmptyText() throws Exception {
        assertTrue(viewModel.validIdentifierIsNotPresentProperty().get());
    }
}
