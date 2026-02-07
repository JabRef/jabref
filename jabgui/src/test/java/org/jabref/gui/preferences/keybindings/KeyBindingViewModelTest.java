package org.jabref.gui.preferences.keybindings;

import java.util.Optional;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.DialogService;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.support.DisabledOnCIServer;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeyBindingViewModelTest {

    @Test
    @DisabledOnCIServer("locally runs fine")
    void resetToDefault() {
        // Set new key binding
        KeyBindingRepository keyBindingRepository = new KeyBindingRepository();
        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getKeyBindingRepository()).thenReturn(keyBindingRepository);
        Injector.setModelOrService(CliPreferences.class, preferences);

        KeyBindingsTabViewModel keyBindingsTabViewModel = new KeyBindingsTabViewModel(keyBindingRepository, mock(DialogService.class), preferences);
        KeyBinding binding = KeyBinding.MERGE_ENTRIES;

        KeyBindingViewModel viewModel = new KeyBindingViewModel(keyBindingRepository, binding, binding.getDefaultKeyBinding());
        keyBindingsTabViewModel.selectedKeyBindingProperty().set(Optional.of(viewModel));

        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "F1", "F1", KeyCode.F1, true, false, false,
                false);

        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.MERGE_ENTRIES, shortcutKeyEvent));

        keyBindingsTabViewModel.setNewBindingForCurrent(shortcutKeyEvent);
        keyBindingsTabViewModel.storeSettings();

        assertTrue(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.MERGE_ENTRIES, shortcutKeyEvent));

        // Reset to default
        viewModel.resetToDefault();

        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.MERGE_ENTRIES, shortcutKeyEvent));
    }

    @Test
    @DisabledOnCIServer("locally runs fine")
    void verifyStoreSettingsWritesChanges() {
        KeyBindingRepository uiRepo = new KeyBindingRepository();
        GuiPreferences preferences = mock(GuiPreferences.class);
        KeyBindingRepository prefsRepo = new KeyBindingRepository();

        when(preferences.getKeyBindingRepository()).thenReturn(prefsRepo);

        KeyBindingsTabViewModel viewModel =
                new KeyBindingsTabViewModel(uiRepo, mock(DialogService.class), preferences);

        KeyBinding binding = KeyBinding.CLOSE_DATABASE;

        KeyBindingViewModel selectedVM = new KeyBindingViewModel(uiRepo, binding, binding.getDefaultKeyBinding());
        viewModel.selectedKeyBindingProperty().set(Optional.of(selectedVM));

        KeyEvent event = new KeyEvent(
                KeyEvent.KEY_PRESSED,
                "L",
                "L",
                KeyCode.L,
                true,
                false,
                false,
                true
        );

        viewModel.setNewBindingForCurrent(event);

        viewModel.storeSettings();

        Optional<String> saved = prefsRepo.get(binding);
        assertEquals(Optional.of("shortcut+shift+L"), saved);
    }
}
