package org.jabref.gui.keyboard;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.keybindings.KeyBindingsTabViewModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class KeyBindingsRestartWarningTest {

    private KeyBindingsTabViewModel viewModel;

    @BeforeEach
    void setUp() {
        KeyBindingRepository keyBindingRepository = new KeyBindingRepository();
        viewModel = new KeyBindingsTabViewModel(keyBindingRepository, mock(DialogService.class));
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
