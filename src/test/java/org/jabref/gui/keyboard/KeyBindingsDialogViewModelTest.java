package org.jabref.gui.keyboard;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.DialogService;
import org.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Test class for the keybindings dialog view model
 *
 */
public class KeyBindingsDialogViewModelTest {

    private KeyBindingsDialogViewModel model;
    private KeyBindingPreferences keyBindingsPreferences;
    private KeyBindingRepository keyBindingRepository ;
    private DialogService dialogService;


    @Before
    public void setUp() {
        JabRefPreferences mockedPreferences = mock(JabRefPreferences.class);
        keyBindingsPreferences = new KeyBindingPreferences(mockedPreferences);
        keyBindingRepository = new KeyBindingRepository(keyBindingsPreferences.getKeyBindings());
        dialogService = mock(DialogService.class);
        model = new KeyBindingsDialogViewModel(keyBindingsPreferences, dialogService);
    }

    @Test
    public void testInvalidKeyBindingIsNotSaved() {
        setKeyBindingViewModel(KeyBinding.COPY);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_RELEASED, "Q", "Q", KeyCode.Q, false, false, false,
                false);
        assertFalse(keyBindingsPreferences.checkKeyCombinationEquality(KeyBinding.COPY, shortcutKeyEvent));
        model.setNewBindingForCurrent(shortcutKeyEvent);
        KeyCombination combination = KeyCombination.keyCombination(keyBindingRepository.get(KeyBinding.COPY).get());
        assertFalse(keyBindingsPreferences.checkKeyCombinationEquality(combination, shortcutKeyEvent));
        model.saveKeyBindings();
        assertFalse(keyBindingsPreferences.checkKeyCombinationEquality(KeyBinding.COPY, shortcutKeyEvent));
    }

    @Test
    @Ignore
    public void testSpecialKeysValidKeyBindingIsSaved() {
        setKeyBindingViewModel(KeyBinding.IMPORT_INTO_NEW_DATABASE);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_RELEASED, "F1", "F1", KeyCode.F1, false, false, false,
                false);
        assertFalse(keyBindingsPreferences.checkKeyCombinationEquality(KeyBinding.IMPORT_INTO_NEW_DATABASE,
                shortcutKeyEvent));
        model.setNewBindingForCurrent(shortcutKeyEvent);

        KeyCombination combination = KeyCombination
                .keyCombination(keyBindingRepository.get(KeyBinding.IMPORT_INTO_NEW_DATABASE).get());

        assertTrue(keyBindingsPreferences.checkKeyCombinationEquality(combination, shortcutKeyEvent));

        model.saveKeyBindings();

        assertTrue(keyBindingsPreferences.checkKeyCombinationEquality(KeyBinding.IMPORT_INTO_NEW_DATABASE,
                shortcutKeyEvent));
    }

    @Test
    public void testKeyBindingCategory() {
        KeyBindingViewModel bindViewModel = new KeyBindingViewModel(keyBindingRepository, KeyBindingCategory.FILE);
        model.selectedKeyBindingProperty().set(bindViewModel);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "M", "M", KeyCode.M, true, true, true, false);
        assertFalse(keyBindingsPreferences.checkKeyCombinationEquality(KeyBinding.CLEANUP, shortcutKeyEvent));
        model.setNewBindingForCurrent(shortcutKeyEvent);
        assertNull(model.selectedKeyBindingProperty().get().getKeyBinding());
    }

    @Test
    @Ignore
    public void testRandomNewKeyKeyBindingInRepository() {
        setKeyBindingViewModel(KeyBinding.CLEANUP);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "K", "K", KeyCode.K, true, true, true, false);
        assertFalse(keyBindingsPreferences.checkKeyCombinationEquality(KeyBinding.CLEANUP, shortcutKeyEvent));
        model.setNewBindingForCurrent(shortcutKeyEvent);

        KeyCombination combination = KeyCombination.keyCombination(keyBindingRepository.get(KeyBinding.CLEANUP).get());

        assertTrue(keyBindingsPreferences.checkKeyCombinationEquality(combination, shortcutKeyEvent));

        assertFalse(keyBindingsPreferences.checkKeyCombinationEquality(KeyBinding.CLEANUP, shortcutKeyEvent));
    }

    @Test
    public void testSaveNewKeyBindingsToPreferences() {
        setKeyBindingViewModel(KeyBinding.ABBREVIATE);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "J", "J", KeyCode.J, true, true, true, false);
        assertFalse(keyBindingsPreferences.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));
        model.setNewBindingForCurrent(shortcutKeyEvent);

        model.saveKeyBindings();

        assertTrue(keyBindingsPreferences.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));
    }

    @Test
    public void testSaveNewSpecialKeysKeyBindingsToPreferences() {
        setKeyBindingViewModel(KeyBinding.UNMARK_ENTRIES);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "F1", "F1", KeyCode.F1, true, false, false,
                false);

        assertFalse(keyBindingsPreferences.checkKeyCombinationEquality(KeyBinding.UNMARK_ENTRIES, shortcutKeyEvent));
        model.setNewBindingForCurrent(shortcutKeyEvent);

        model.saveKeyBindings();

        assertTrue(keyBindingsPreferences.checkKeyCombinationEquality(KeyBinding.UNMARK_ENTRIES, shortcutKeyEvent));
    }

    @Test
    @Ignore
    public void testSetAllKeyBindingsToDefault() {
        setKeyBindingViewModel(KeyBinding.ABBREVIATE);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "C", "C", KeyCode.C, true, true, true, false);

        assertFalse(keyBindingsPreferences.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));

        model.setNewBindingForCurrent(shortcutKeyEvent);
        model.saveKeyBindings();

        assertTrue(keyBindingsPreferences.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));

        keyBindingRepository.resetToDefault();
        model.saveKeyBindings();

        assertFalse(keyBindingsPreferences.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));
    }

    @Test
    @Ignore
    public void testSetSingleKeyBindingToDefault() {
        KeyBindingViewModel viewModel = setKeyBindingViewModel(KeyBinding.ABBREVIATE);
        model.selectedKeyBindingProperty().set(viewModel);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "C", "C", KeyCode.C, true, true, true, false);

        assertFalse(keyBindingsPreferences.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));

        model.setNewBindingForCurrent(shortcutKeyEvent);
        model.saveKeyBindings();

        assertTrue(keyBindingsPreferences.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));

        viewModel.resetToDefault();
        model.saveKeyBindings();

        assertFalse(keyBindingsPreferences.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));
    }

    private KeyBindingViewModel setKeyBindingViewModel(KeyBinding binding) {
        KeyBindingViewModel bindViewModel = new KeyBindingViewModel(keyBindingRepository, binding, binding.getDefaultBinding());
        model.selectedKeyBindingProperty().set(bindViewModel);
        return bindViewModel;
    }

}
