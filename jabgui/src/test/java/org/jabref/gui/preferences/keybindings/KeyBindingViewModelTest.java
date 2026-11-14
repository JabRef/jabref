package org.jabref.gui.preferences.keybindings;

import java.util.Optional;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.DialogService;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preferences.keybindings.presets.BashKeyBindingPreset;
import org.jabref.gui.preferences.keybindings.presets.MacOsKeyBindingPreset;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.support.DisabledOnCIServer;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeyBindingViewModelTest {

    @Test
    void macOsPresetClearsBashBindingsAndPreservesConflictingCustomShortcut() {
        KeyBindingRepository repository = new KeyBindingRepository();
        repository.put(KeyBinding.CLEANUP, "ctrl+A");
        KeyBindingsTabViewModel viewModel = new KeyBindingsTabViewModel(repository, mock(DialogService.class));

        viewModel.loadPreset(new BashKeyBindingPreset());
        viewModel.loadPreset(new MacOsKeyBindingPreset());

        assertEquals(Optional.of("ctrl+A"), viewModel.getKeyBindingRepository().get(KeyBinding.CLEANUP));
        assertEquals(Optional.of(""), viewModel.getKeyBindingRepository().get(KeyBinding.EDITOR_BEGINNING));
        assertEquals(Optional.of(""), viewModel.getKeyBindingRepository().get(KeyBinding.EDITOR_WORD_FORWARD));
        assertEquals(Optional.of("ctrl+T"), viewModel.getKeyBindingRepository().get(KeyBinding.EDITOR_TRANSPOSE_CHARACTERS));
    }

    @Test
    @DisabledOnCIServer("locally runs fine")
    void resetToDefault() {
        // Set new key binding
        KeyBindingRepository keyBindingRepository = new KeyBindingRepository();
        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getKeyBindingRepository()).thenReturn(keyBindingRepository);
        Injector.setModelOrService(CliPreferences.class, preferences);

        KeyBindingsTabViewModel keyBindingsTabViewModel = new KeyBindingsTabViewModel(keyBindingRepository, mock(DialogService.class));
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
    @EnabledOnOs(OS.MAC)
    void controlBackspaceIsRecordedAsBindingOnMacOS() {
        KeyBindingRepository liveRepo = new KeyBindingRepository();

        KeyBindingsTabViewModel viewModel =
                new KeyBindingsTabViewModel(liveRepo, mock(DialogService.class));

        KeyBinding binding = KeyBinding.EDITOR_KILL_WORD_BACKWARD;

        KeyBindingViewModel selectedVM =
                new KeyBindingViewModel(
                        viewModel.getKeyBindingRepository(),
                        binding,
                        binding.getDefaultKeyBinding());

        viewModel.selectedKeyBindingProperty().set(Optional.of(selectedVM));

        KeyEvent event = new KeyEvent(
                KeyEvent.KEY_PRESSED,
                "",
                "",
                KeyCode.BACK_SPACE,
                false,
                true,
                false,
                false
        );

        viewModel.setNewBindingForCurrent(event);
        viewModel.storeSettings();

        Optional<String> saved = liveRepo.get(binding);

        assertEquals(Optional.of("ctrl+BACK_SPACE"), saved);
    }

    @Test
    @DisabledOnCIServer("locally runs fine")
    void verifyStoreSettingsWritesChanges() {
        KeyBindingRepository liveRepo = new KeyBindingRepository();

        KeyBindingsTabViewModel viewModel =
                new KeyBindingsTabViewModel(liveRepo, mock(DialogService.class));

        KeyBinding binding = KeyBinding.CLOSE_DATABASE;

        KeyBindingViewModel selectedVM = new KeyBindingViewModel(viewModel.getKeyBindingRepository(), binding, binding.getDefaultKeyBinding());
        viewModel.selectedKeyBindingProperty().set(Optional.of(selectedVM));

        // Shift+Shortcut+J is not used by any default binding, so the duplicate warning does not interfere
        KeyEvent event = new KeyEvent(
                KeyEvent.KEY_PRESSED,
                "J",
                "J",
                KeyCode.J,
                true,
                false,
                false,
                true
        );

        viewModel.setNewBindingForCurrent(event);

        viewModel.storeSettings();

        Optional<String> saved = liveRepo.get(binding);
        assertEquals(Optional.of("shortcut+shift+J"), saved);
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void controlKeyIsRecordedAsBindingOnMacOs() {
        KeyBindingRepository liveRepo = new KeyBindingRepository();

        KeyBindingsTabViewModel viewModel =
                new KeyBindingsTabViewModel(liveRepo, mock(DialogService.class));

        KeyBinding binding = KeyBinding.CLOSE_DATABASE;

        KeyBindingViewModel selectedVM = new KeyBindingViewModel(viewModel.getKeyBindingRepository(), binding, binding.getDefaultKeyBinding());
        viewModel.selectedKeyBindingProperty().set(Optional.of(selectedVM));

        // Control (distinct from Shortcut/Cmd on macOS) held with 'J', no other modifiers.
        // 'J' is used, because Control plus most other letters are default macOS text-editing shortcuts
        KeyEvent event = new KeyEvent(
                KeyEvent.KEY_PRESSED,
                "J",
                "J",
                KeyCode.J,
                false,
                true,
                false,
                false
        );

        viewModel.setNewBindingForCurrent(event);

        viewModel.storeSettings();

        Optional<String> saved = liveRepo.get(binding);
        assertEquals(Optional.of("ctrl+J"), saved);
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void controlKeyCombinedWithShiftIsRecordedAsBindingOnMacOs() {
        KeyBindingRepository liveRepo = new KeyBindingRepository();

        KeyBindingsTabViewModel viewModel =
                new KeyBindingsTabViewModel(liveRepo, mock(DialogService.class));

        KeyBinding binding = KeyBinding.CLOSE_DATABASE;

        KeyBindingViewModel selectedVM = new KeyBindingViewModel(viewModel.getKeyBindingRepository(), binding, binding.getDefaultKeyBinding());
        viewModel.selectedKeyBindingProperty().set(Optional.of(selectedVM));

        // Control and Shift held together with 'A'
        KeyEvent event = new KeyEvent(
                KeyEvent.KEY_PRESSED,
                "A",
                "A",
                KeyCode.A,
                true,
                true,
                false,
                false
        );

        viewModel.setNewBindingForCurrent(event);

        viewModel.storeSettings();

        Optional<String> saved = liveRepo.get(binding);
        assertEquals(Optional.of("shift+ctrl+A"), saved);
    }
}
