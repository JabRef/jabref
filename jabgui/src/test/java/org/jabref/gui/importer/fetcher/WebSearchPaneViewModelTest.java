package org.jabref.gui.importer.fetcher;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.importer.ImportEntriesDialog;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class WebSearchPaneViewModelTest {

    private GuiPreferences preferences;
    private DialogService dialogService;
    private StateManager stateManager;
    private WebSearchPaneViewModel viewModel;

    @BeforeEach
    void setUp() {
        preferences = Mockito.mock(GuiPreferences.class, RETURNS_DEEP_STUBS);
        dialogService = Mockito.mock(DialogService.class);
        stateManager = Mockito.mock(StateManager.class);

        viewModel = new WebSearchPaneViewModel(preferences, dialogService, stateManager);
    }

    @Test
    void queryConsistingOfASingleAndIsNotValid() {
        viewModel.queryProperty().setValue("AND");
        assertFalse(viewModel.queryValidationStatus().validProperty().getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"test query", "10.1007/JHEP02(2023)082", "arXiv:2110.02957"})
    void searchSelectsWebSearchDownloadPreferenceBeforeShowingDialog(String query) {
        // [utest->req~import.dialog.download-linked-files~1]
        when(preferences.getImporterPreferences().areImporterEnabled()).thenReturn(true);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(new BibDatabaseContext()));
        SearchBasedFetcher fetcher = mock(SearchBasedFetcher.class);
        when(fetcher.getName()).thenReturn("Test fetcher");
        viewModel.selectedFetcherProperty().set(fetcher);
        viewModel.queryProperty().set(query);

        try (MockedConstruction<ImportEntriesDialog> dialogs = mockConstruction(ImportEntriesDialog.class)) {
            viewModel.search();

            assertEquals(1, dialogs.constructed().size());
            ImportEntriesDialog dialog = dialogs.constructed().getFirst();
            InOrder order = inOrder(dialog, dialogService);
            order.verify(dialog).useWebSearchDownloadPreference();
            order.verify(dialogService).showCustomDialogAndWait(dialog);
        }
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
    void queryConsistingOfInvalidDOIIsValid() {
        viewModel.queryProperty().setValue("101.1007/JHEP02(2023)082");
        // There is currently no interpretation of nearly-valid identifiers, therefore, this is considered as "regular" search term
        assertTrue(viewModel.queryValidationStatus().validProperty().getValue());
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
        viewModel.queryProperty().setValue("arXiv=2110.02957");
        assertTrue(viewModel.queryValidationStatus().validProperty().getValue());
    }

    @Test
    void canExtractArXivIdFromQueryText() {
        viewModel.queryProperty().setValue("this query contains an ArXiv identifier 2110.02957");
        assertTrue(viewModel.queryValidationStatus().validProperty().getValue());
    }
}
