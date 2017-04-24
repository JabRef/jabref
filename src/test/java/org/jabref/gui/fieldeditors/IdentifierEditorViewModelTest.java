package org.jabref.gui.fieldeditors;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.CurrentThreadTaskExecutor;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class IdentifierEditorViewModelTest {

    private IdentifierEditorViewModel viewModel;

    @Before
    public void setUp() throws Exception {
        viewModel = new IdentifierEditorViewModel("DOI", new CurrentThreadTaskExecutor(), mock(DialogService.class));
    }

    @Test
    public void validIdentifierIsNotPresentIsTrueForEmptyText() throws Exception {
        assertTrue(viewModel.validIdentifierIsNotPresentProperty().get());
    }
}
