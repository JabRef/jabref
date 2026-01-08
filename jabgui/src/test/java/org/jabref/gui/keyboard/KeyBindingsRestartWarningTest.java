package org.jabref.gui.keyboard;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preferences.keybindings.KeyBindingsTabViewModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeyBindingsRestartWarningTest {

    private KeyBindingsTabViewModel viewModel;

    @BeforeEach
    void setUp() {
        KeyBindingRepository keyBindingRepository = new KeyBindingRepository();
        GuiPreferences preferences = mock(GuiPreferences.class);
        // Ensure the preferences repo is a copy so that they are initially "equal" but different instances
        KeyBindingRepository prefsRepo = new KeyBindingRepository(keyBindingRepository.getKeyBindings());
        when(preferences.getKeyBindingRepository()).thenReturn(prefsRepo);

        viewModel = new KeyBindingsTabViewModel(keyBindingRepository, mock(DialogService.class), preferences);
    }

    @Test
    void storeSettingsDoesNotAddRestartWarningWhenNoChanges() {
        viewModel.storeSettings();
        assertEquals(List.of(), viewModel.getRestartWarnings());
    }

    @Test
    void storeSettingsAddsRestartWarningWhenChangesMade() {
        viewModel.getKeyBindingRepository().put(KeyBinding.COPY, "Ctrl+Q");
        viewModel.storeSettings();
        assertFalse(viewModel.getRestartWarnings().isEmpty());
    }
}
