package org.jabref.gui.keyboard;

import java.util.Optional;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.keybindings.KeyBindingViewModel;
import org.jabref.gui.preferences.keybindings.KeyBindingsTabViewModel;
import org.jabref.logic.util.OS;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.Mockito.mock;

/**
 * Test class for the keybindings dialog view model
 */
class KeyBindingsTabModelTest {

    private KeyBindingsTabViewModel model;
    private KeyBindingRepository keyBindingRepository;

    @BeforeEach
    void setUp() {
        keyBindingRepository = new KeyBindingRepository();
        model = new KeyBindingsTabViewModel(keyBindingRepository, mock(DialogService.class), mock(PreferencesService.class));
    }

    @Test
    void testInvalidKeyBindingIsNotSaved() {
        setKeyBindingViewModel(KeyBinding.COPY);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_RELEASED, "Q", "Q", KeyCode.Q, false, false, false,
                false);
        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.COPY, shortcutKeyEvent));
        model.setNewBindingForCurrent(shortcutKeyEvent);
        KeyCombination combination = KeyCombination.keyCombination(keyBindingRepository.get(KeyBinding.COPY).get());
        assertFalse(KeyBindingRepository.checkKeyCombinationEquality(combination, shortcutKeyEvent));
        model.storeSettings();
        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.COPY, shortcutKeyEvent));
    }

    @Test
    void testSpecialKeysValidKeyBindingIsSaved() {
        setKeyBindingViewModel(KeyBinding.IMPORT_INTO_NEW_DATABASE);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_RELEASED, "F1", "F1", KeyCode.F1, false, false, false,
                false);
        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.IMPORT_INTO_NEW_DATABASE,
                shortcutKeyEvent));
        model.setNewBindingForCurrent(shortcutKeyEvent);

        KeyCombination combination = KeyCombination
                .keyCombination(keyBindingRepository.get(KeyBinding.IMPORT_INTO_NEW_DATABASE).get());

        assertTrue(KeyBindingRepository.checkKeyCombinationEquality(combination, shortcutKeyEvent));

        model.storeSettings();

        assertTrue(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.IMPORT_INTO_NEW_DATABASE,
                                                                    shortcutKeyEvent));
    }

    @Test
    void testKeyBindingCategory() {
        KeyBindingViewModel bindViewModel = new KeyBindingViewModel(keyBindingRepository, KeyBindingCategory.FILE);
        model.selectedKeyBindingProperty().set(Optional.of(bindViewModel));
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "M", "M", KeyCode.M, true, true, true, false);
        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.CLEANUP, shortcutKeyEvent));
        model.setNewBindingForCurrent(shortcutKeyEvent);
        assertNull(model.selectedKeyBindingProperty().get().get().getKeyBinding());
    }

    @Test
    void testRandomNewKeyKeyBindingInRepository() {
        setKeyBindingViewModel(KeyBinding.CLEANUP);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "K", "K", KeyCode.K, true, true, true, false);
        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.CLEANUP, shortcutKeyEvent));
        model.setNewBindingForCurrent(shortcutKeyEvent);

        KeyCombination combination = KeyCombination.keyCombination(keyBindingRepository.get(KeyBinding.CLEANUP).get());

        assertTrue(KeyBindingRepository.checkKeyCombinationEquality(combination, shortcutKeyEvent));

        assertFalse(KeyBindingRepository.checkKeyCombinationEquality(KeyCombination.valueOf(KeyBinding.CLEANUP.getDefaultKeyBinding()), shortcutKeyEvent));
    }

    @Test
    void testSaveNewKeyBindingsToPreferences() {
        assumeFalse(OS.OS_X);

        setKeyBindingViewModel(KeyBinding.ABBREVIATE);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "J", "J", KeyCode.J, true, true, true, false);
        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));
        model.setNewBindingForCurrent(shortcutKeyEvent);

        model.storeSettings();

        assertTrue(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));
    }

    @Test
    void testSaveNewSpecialKeysKeyBindingsToPreferences() {
        setKeyBindingViewModel(KeyBinding.ABBREVIATE);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "F1", "F1", KeyCode.F1, true, false, false,
                false);

        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));
        model.setNewBindingForCurrent(shortcutKeyEvent);

        model.storeSettings();

        assertTrue(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));
    }

    @Test
    void testSetAllKeyBindingsToDefault() {
        assumeFalse(OS.OS_X);

        setKeyBindingViewModel(KeyBinding.ABBREVIATE);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "C", "C", KeyCode.C, true, true, true, false);

        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));

        model.setNewBindingForCurrent(shortcutKeyEvent);
        model.storeSettings();

        assertTrue(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));

        keyBindingRepository.resetToDefault();
        model.storeSettings();

        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));
    }

    @Test
    void testCloseEntryEditorCloseEntryKeybinding() {
        KeyBindingViewModel viewModel = setKeyBindingViewModel(KeyBinding.CLOSE);
        model.selectedKeyBindingProperty().set(Optional.of(viewModel));
        KeyEvent closeEditorEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false);

        assertEquals(KeyBinding.CLOSE.getDefaultKeyBinding(), KeyCode.ESCAPE.getName());

        KeyCombination combi = KeyCombination.valueOf(KeyBinding.CLOSE.getDefaultKeyBinding());

        assertTrue(combi.match(closeEditorEvent));
        assertTrue(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.CLOSE, closeEditorEvent));
    }

    @Test
    void testSetSingleKeyBindingToDefault() {
        assumeFalse(OS.OS_X);

        KeyBindingViewModel viewModel = setKeyBindingViewModel(KeyBinding.ABBREVIATE);
        model.selectedKeyBindingProperty().set(Optional.of(viewModel));
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "C", "C", KeyCode.C, true, true, true, false);

        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));

        model.setNewBindingForCurrent(shortcutKeyEvent);
        model.storeSettings();

        assertTrue(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));

        viewModel.resetToDefault();
        model.storeSettings();

        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));
    }

    private KeyBindingViewModel setKeyBindingViewModel(KeyBinding binding) {
        KeyBindingViewModel viewModel = new KeyBindingViewModel(keyBindingRepository, binding, binding.getDefaultKeyBinding());
        model.selectedKeyBindingProperty().set(Optional.of(viewModel));
        return viewModel;
    }
}
