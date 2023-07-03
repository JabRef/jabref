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

    @Test
    void queryConsistingOfDOIIsValid() {
        viewModel.queryProperty().setValue("10.1007/JHEP02(2023)082");
        assertTrue(viewModel.queryValidationStatus().validProperty().getValue());
    }

    @Test
    void canExtractDOIFromQueryText() {
        viewModel.queryProperty().setValue("this is the DOI: 10.1007/JHEP02(2023)082, other text");
        assertTrue(viewModel.queryValidationStatus().validProperty().getValue());
    }

    @Test
    void queryConsistingOfInvalidDOIIsInvalid() {
        viewModel.queryProperty().setValue("101.1007/JHEP02(2023)082");
        assertFalse(viewModel.queryValidationStatus().validProperty().getValue());
    }

    @Test
    void queryConsistingOfISBNIsValid() {
        viewModel.queryProperty().setValue("9780134685991");
        assertTrue(viewModel.queryValidationStatus().validProperty().getValue());
    }

    @Test
    void canExtractISBNFromQueryText() {
        viewModel.queryProperty().setValue(";:isbn (9780134685991), text2");
        assertTrue(viewModel.queryValidationStatus().validProperty().getValue());
    }

    @Test
    void queryConsistingOfArXivIdIsValid() {
        viewModel.queryProperty().setValue("arXiv:2110.02957");
        assertTrue(viewModel.queryValidationStatus().validProperty().getValue());
    }

    @Test
    void canExtractArXivIdFromQueryText() {
        viewModel.queryProperty().setValue("this query contains an ArXiv identifier 2110.02957");
        assertTrue(viewModel.queryValidationStatus().validProperty().getValue());
    }
}
