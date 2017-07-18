package org.jabref.gui.keyboard;

import java.awt.event.InputEvent;
import java.util.Optional;

import javax.swing.JFrame;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.DialogService;
import org.jabref.preferences.PreferencesService;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
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
    private KeyBindingRepository keyBindingRepository;
    private DialogService dialogService;

    @Before
    public void setUp() {
        keyBindingRepository = new KeyBindingRepository();
        dialogService = mock(DialogService.class);
        model = new KeyBindingsDialogViewModel(keyBindingRepository, dialogService, mock(PreferencesService.class));
    }

    @Test
    public void testInvalidKeyBindingIsNotSaved() {
        setKeyBindingViewModel(KeyBinding.COPY);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_RELEASED, "Q", "Q", KeyCode.Q, false, false, false,
                false);
        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.COPY, shortcutKeyEvent));
        model.setNewBindingForCurrent(shortcutKeyEvent);
        KeyCombination combination = KeyCombination.keyCombination(keyBindingRepository.get(KeyBinding.COPY).get());
        assertFalse(keyBindingRepository.checkKeyCombinationEquality(combination, shortcutKeyEvent));
        model.saveKeyBindings();
        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.COPY, shortcutKeyEvent));
    }

    @Test
    public void testSpecialKeysValidKeyBindingIsSaved() {
        setKeyBindingViewModel(KeyBinding.IMPORT_INTO_NEW_DATABASE);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_RELEASED, "F1", "F1", KeyCode.F1, false, false, false,
                false);
        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.IMPORT_INTO_NEW_DATABASE,
                shortcutKeyEvent));
        model.setNewBindingForCurrent(shortcutKeyEvent);

        KeyCombination combination = KeyCombination
                .keyCombination(keyBindingRepository.get(KeyBinding.IMPORT_INTO_NEW_DATABASE).get());

        assertTrue(keyBindingRepository.checkKeyCombinationEquality(combination, shortcutKeyEvent));

        model.saveKeyBindings();

        assertTrue(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.IMPORT_INTO_NEW_DATABASE,
                shortcutKeyEvent));
    }

    @Test
    public void testKeyBindingCategory() {
        KeyBindingViewModel bindViewModel = new KeyBindingViewModel(keyBindingRepository, KeyBindingCategory.FILE);
        model.selectedKeyBindingProperty().set(bindViewModel);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "M", "M", KeyCode.M, true, true, true, false);
        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.CLEANUP, shortcutKeyEvent));
        model.setNewBindingForCurrent(shortcutKeyEvent);
        assertNull(model.selectedKeyBindingProperty().get().getKeyBinding());
    }

    @Test
    public void testRandomNewKeyKeyBindingInRepository() {
        setKeyBindingViewModel(KeyBinding.CLEANUP);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "K", "K", KeyCode.K, true, true, true, false);
        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.CLEANUP, shortcutKeyEvent));
        model.setNewBindingForCurrent(shortcutKeyEvent);

        KeyCombination combination = KeyCombination.keyCombination(keyBindingRepository.get(KeyBinding.CLEANUP).get());

        assertTrue(keyBindingRepository.checkKeyCombinationEquality(combination, shortcutKeyEvent));

        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyCombination.valueOf(KeyBinding.CLEANUP.getDefaultKeyBinding()), shortcutKeyEvent));
    }

    @Test
    public void testSaveNewKeyBindingsToPreferences() {
        setKeyBindingViewModel(KeyBinding.ABBREVIATE);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "J", "J", KeyCode.J, true, true, true, false);
        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));
        model.setNewBindingForCurrent(shortcutKeyEvent);

        model.saveKeyBindings();

        assertTrue(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));
    }

    @Test
    public void testSaveNewSpecialKeysKeyBindingsToPreferences() {
        setKeyBindingViewModel(KeyBinding.UNMARK_ENTRIES);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "F1", "F1", KeyCode.F1, true, false, false,
                false);

        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.UNMARK_ENTRIES, shortcutKeyEvent));
        model.setNewBindingForCurrent(shortcutKeyEvent);

        model.saveKeyBindings();

        assertTrue(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.UNMARK_ENTRIES, shortcutKeyEvent));
    }

    @Test
    public void testSetAllKeyBindingsToDefault() {
        setKeyBindingViewModel(KeyBinding.ABBREVIATE);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "C", "C", KeyCode.C, true, true, true, false);

        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));

        model.setNewBindingForCurrent(shortcutKeyEvent);
        model.saveKeyBindings();

        assertTrue(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));

        keyBindingRepository.resetToDefault();
        model.saveKeyBindings();

        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));
    }

    @Test
    public void testCloseEntryEditorCloseEntryKeybinding() {
        KeyBindingViewModel viewModel = setKeyBindingViewModel(KeyBinding.CLOSE_ENTRY_EDITOR);
        model.selectedKeyBindingProperty().set(viewModel);
        KeyEvent closeEditorEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false);

        assertEquals(KeyBinding.CLOSE_ENTRY_EDITOR.getDefaultKeyBinding(), KeyCode.ESCAPE.getName());

        KeyCombination combi = KeyCombination.valueOf(KeyBinding.CLOSE_ENTRY_EDITOR.getDefaultKeyBinding());

        assertTrue(combi.match(closeEditorEvent));
        assertTrue(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.CLOSE_ENTRY_EDITOR, closeEditorEvent));

    }

    @Test
    public void testSetSingleKeyBindingToDefault() {
        KeyBindingViewModel viewModel = setKeyBindingViewModel(KeyBinding.ABBREVIATE);
        model.selectedKeyBindingProperty().set(viewModel);
        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "C", "C", KeyCode.C, true, true, true, false);

        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));

        model.setNewBindingForCurrent(shortcutKeyEvent);
        model.saveKeyBindings();

        assertTrue(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));

        viewModel.resetToDefault();
        model.saveKeyBindings();

        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));
    }

    @Test
    public void testConversionAwtKeyEventJavafxKeyEvent() {
        java.awt.event.KeyEvent evt = new java.awt.event.KeyEvent(mock(JFrame.class), 0, 0, InputEvent.CTRL_MASK, java.awt.event.KeyEvent.VK_S, java.awt.event.KeyEvent.CHAR_UNDEFINED);

        Optional<KeyBinding> keyBinding = keyBindingRepository.mapToKeyBinding(evt);
        assertEquals(Optional.of(KeyBinding.SAVE_DATABASE), keyBinding);
    }

    private KeyBindingViewModel setKeyBindingViewModel(KeyBinding binding) {
        KeyBindingViewModel bindViewModel = new KeyBindingViewModel(keyBindingRepository, binding, binding.getDefaultKeyBinding());
        model.selectedKeyBindingProperty().set(bindViewModel);
        return bindViewModel;
    }

}
