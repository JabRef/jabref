package org.jabref.gui.importer.fetcher;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

class WebSearchPaneViewModelTest {

    private PreferencesService preferencesService;
    private DialogService dialogService;
    private StateManager stateManager;
    private WebSearchPaneViewModel viewModel;

    @BeforeEach
    void setUp() {
        preferencesService = Mockito.mock(PreferencesService.class, RETURNS_DEEP_STUBS);
        dialogService = Mockito.mock(DialogService.class);
        stateManager = Mockito.mock(StateManager.class);

        viewModel = new WebSearchPaneViewModel(preferencesService, dialogService, stateManager);
    }

    @Test
    void queryConsistingOfASingleAndIsNotValid() {
        viewModel.queryProperty().setValue("AND");
        assertFalse(viewModel.queryValidationStatus().validProperty().getValue());
    }

    @Test
    void falseQueryValidationStatus() {
        viewModel.queryProperty().setValue("Miami !Beach AND OR Blue");
        assertFalse(viewModel.queryValidationStatus().validProperty().getValue());
    }

     @Test
     void correctQueryValidationStatus() {
        viewModel.queryProperty().setValue("Miami AND Beach OR Houston AND Texas");
        assertTrue(viewModel.queryValidationStatus().validProperty().getValue());
    }

    @Test
    void notFalseQueryValidationStatus() {
        viewModel.queryProperty().setValue("Miami !Beach AND OR Blue");
        assertTrue(viewModel.queryValidationStatus().validProperty().not().getValue());
    }

    @Test
    void notCorrectQueryValidationStatus() {
        viewModel.queryProperty().setValue("Miami AND Beach OR Houston AND Texas");
        assertFalse(viewModel.queryValidationStatus().validProperty().not().getValue());
    }
}
