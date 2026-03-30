package org.jabref.gui.preferences.keybindings;

import java.util.Optional;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.DialogService;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.preferences.CliPreferences;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Test for GitHub issue #7959: Key bindings changes should not persist when pressing Cancel
class KeyBindingsCancelBugTest {

    @Test
    void verifyCancelDoesNotPersistChanges() {
        // Setup: Create the preferences repository that will be stored
        KeyBindingRepository prefsRepo = new KeyBindingRepository();
        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getKeyBindingRepository()).thenReturn(prefsRepo);
        Injector.setModelOrService(CliPreferences.class, preferences);

        // Record original value
        KeyBinding binding = KeyBinding.CLOSE_DATABASE;
        Optional<String> originalValue = prefsRepo.get(binding);

        // User opens Key Bindings dialog
        // ViewModel creates a temp copy of preferences' repository
        KeyBindingsTabViewModel viewModel
                = new KeyBindingsTabViewModel(prefsRepo, mock(DialogService.class), preferences);
        viewModel.setValues();

        // Get a key binding view model for editing
        KeyBindingViewModel selectedVM = new KeyBindingViewModel(
                viewModel.getKeyBindingRepository(),
                binding,
                binding.getDefaultKeyBinding()
        );
        viewModel.selectedKeyBindingProperty().set(Optional.of(selectedVM));

        // User clicks CLEAR button (broom icon)
        selectedVM.clear();

        // Verify that the TEMP copy was modified
        assertEquals("", selectedVM.getBinding(),
                "Clear should have updated the view model");

        // CRITICAL: User clicks CANCEL button - storeSettings() is NOT called
        // (In real UI, this happens when dialog closes without Save)
        // Verify that preferences' repository was NOT modified
        Optional<String> afterCancel = prefsRepo.get(binding);
        assertEquals(originalValue, afterCancel,
                "BUG #7959 FAILED: Preferences were modified even though Cancel was pressed!");

        // Additional check: If user reopens the dialog, values should be unchanged
        KeyBindingsTabViewModel viewModel2
                = new KeyBindingsTabViewModel(prefsRepo, mock(DialogService.class), preferences);
        viewModel2.setValues();

        Optional<String> afterReopen = prefsRepo.get(binding);
        assertEquals(originalValue, afterReopen,
                "After reopening dialog, preferences should not have changed");
    }

    @Test
    void verifySaveDoesPersistChanges() {
        // Setup: Create the preferences repository
        KeyBindingRepository prefsRepo = new KeyBindingRepository();
        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getKeyBindingRepository()).thenReturn(prefsRepo);

        KeyBinding binding = KeyBinding.CLOSE_DATABASE;

        // User opens Key Bindings dialog and makes a change
        KeyBindingsTabViewModel viewModel
                = new KeyBindingsTabViewModel(prefsRepo, mock(DialogService.class), preferences);
        viewModel.setValues();

        KeyBindingViewModel selectedVM = new KeyBindingViewModel(
                viewModel.getKeyBindingRepository(),
                binding,
                binding.getDefaultKeyBinding()
        );
        viewModel.selectedKeyBindingProperty().set(Optional.of(selectedVM));

        // User sets a new key binding
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "F2", "F2", KeyCode.F2, true, false, false, false);
        viewModel.setNewBindingForCurrent(event);

        // User clicks SAVE button - storeSettings() IS called
        viewModel.storeSettings();

        // Verify that preferences' repository WAS modified
        Optional<String> saved = prefsRepo.get(binding);
        assertEquals(Optional.of("shift+F2"), saved,
                "Save should have persisted the changes to preferences");
    }

    @Test
    void verifyClearAndResetInTempCopy() {
        KeyBindingRepository prefsRepo = new KeyBindingRepository();
        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getKeyBindingRepository()).thenReturn(prefsRepo);

        KeyBinding binding = KeyBinding.MERGE_ENTRIES;
        Optional<String> originalValue = prefsRepo.get(binding);

        // Open dialog and reset to default
        KeyBindingsTabViewModel viewModel
                = new KeyBindingsTabViewModel(prefsRepo, mock(DialogService.class), preferences);
        viewModel.setValues();

        KeyBindingViewModel selectedVM = new KeyBindingViewModel(
                viewModel.getKeyBindingRepository(),
                binding,
                binding.getDefaultKeyBinding()
        );

        // Set a new binding in the temp copy
        KeyEvent event = new KeyEvent(KeyEvent.KEY_PRESSED, "X", "X", KeyCode.X, true, true, false, false);
        selectedVM.setNewBinding(event);

        // Reset it back in the temp copy
        selectedVM.resetToDefault();

        // Cancel without saving
        // (storeSettings not called)
        // Verify preferences unchanged
        Optional<String> afterCancel = prefsRepo.get(binding);
        assertEquals(originalValue, afterCancel,
                "After setting and resetting in temp copy then canceling, preferences should be unchanged");
    }
}
