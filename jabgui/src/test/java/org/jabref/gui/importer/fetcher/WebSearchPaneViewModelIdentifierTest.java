package org.jabref.gui.importer.fetcher;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSearchPaneViewModelIdentifierTest {

    private WebSearchPaneViewModel vm() {
        GuiPreferences prefs = mock(GuiPreferences.class, RETURNS_DEEP_STUBS);
        DialogService dialog = mock(DialogService.class);
        StateManager state = mock(StateManager.class);
        return new WebSearchPaneViewModel(prefs, dialog, state);
    }

    @ParameterizedTest
    @CsvSource({
        "10.1145/3368089,DOI",
        "arXiv:2101.00001,ArXiv",
        "978-3-16-148410-0,ISBN"
    })
    void detectsIdentifierType(String input, String expected) {
        var vm = vm();
        vm.queryProperty().set(input);
        assertTrue(vm.isIdentifierDetected());
        assertEquals(expected, vm.getDetectedIdentifierType());
    }
}

