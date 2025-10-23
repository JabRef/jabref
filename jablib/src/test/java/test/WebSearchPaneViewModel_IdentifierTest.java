// src/test/java/org/jabref/gui/importer/fetcher/WebSearchPaneViewModel_IdentifierTest.java
package org.jabref.gui.importer.fetcher;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.logic.preferences.GuiPreferences;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSearchPaneViewModel_IdentifierTest {

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
        "978-3-16-148410-0,ISBN",
        "SSRN:1234567,SSRN",
        "RFC:2616,RFC"
    })
    void detectsIdentifierType(String input, String expected) {
        var vm = vm();
        vm.queryProperty().set(input);
        assertThat(vm.isIdentifierDetected()).isTrue();
        assertThat(vm.getDetectedIdentifierType()).isEqualTo(expected);
    }
}

