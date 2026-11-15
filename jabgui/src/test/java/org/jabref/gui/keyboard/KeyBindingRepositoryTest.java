package org.jabref.gui.keyboard;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.testutils.JavaFxExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(JavaFxExtension.class)
class KeyBindingRepositoryTest {
    private static Stream<Arguments> provideTestData() {
        return Stream.of(
                // Correctly mapped
                Arguments.of(
                        List.of(KeyBinding.MERGE_ENTRIES, KeyBinding.NEW_TECHREPORT, KeyBinding.PASTE),
                        List.of("shortcut+1", "alt+2", "shift+3")
                ),

                // Defaults on faulty data
                Arguments.of(
                        List.of(KeyBinding.MERGE_ENTRIES, KeyBinding.NEW_TECHREPORT, KeyBinding.PASTE),
                        List.of(KeyBinding.MERGE_ENTRIES.getDefaultKeyBinding(), KeyBinding.NEW_TECHREPORT.getDefaultKeyBinding())
                ));
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void parseStringLists(List<KeyBinding> keybindings, List<String> bindings) {
        List<String> bindNames = keybindings.stream().map(KeyBinding::getConstant).toList();
        KeyBindingRepository keyBindingRepository = new KeyBindingRepository(bindNames, bindings);

        assertEquals(keyBindingRepository.get(bindNames.getFirst()), bindings.getFirst());
        assertEquals(keyBindingRepository.get(bindNames.get(1)), bindings.get(1));
    }

    // [utest->req~ux.keybindings.duplicate-warning~1]
    @Test
    void findsConflictingBinding() {
        KeyBindingRepository repo = new KeyBindingRepository(
                List.of(KeyBinding.MERGE_ENTRIES.getConstant(), KeyBinding.PASTE.getConstant()),
                List.of("alt+A", "shift+B"));
        KeyEvent altA = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.A, false, false, true, false);

        List<KeyBinding> conflicts = repo.findConflictingKeyBindings(KeyBinding.PASTE, altA);

        assertEquals(List.of(KeyBinding.MERGE_ENTRIES), conflicts);
    }

    // [utest->req~ux.keybindings.duplicate-warning~1]
    @Test
    void ignoresBindingItself() {
        KeyBindingRepository repo = new KeyBindingRepository(
                List.of(KeyBinding.MERGE_ENTRIES.getConstant(), KeyBinding.PASTE.getConstant()),
                List.of("alt+A", "shift+B"));
        KeyEvent altA = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.A, false, false, true, false);

        List<KeyBinding> conflicts = repo.findConflictingKeyBindings(KeyBinding.MERGE_ENTRIES, altA);

        assertEquals(List.of(), conflicts);
    }

    // [utest->req~ux.keybindings.duplicate-warning~1]
    @Test
    void returnsEmptyForFreeCombination() {
        KeyBindingRepository repo = new KeyBindingRepository(
                List.of(KeyBinding.MERGE_ENTRIES.getConstant(), KeyBinding.PASTE.getConstant()),
                List.of("alt+A", "shift+B"));
        KeyEvent altZ = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.Z, false, false, true, false);

        List<KeyBinding> conflicts = repo.findConflictingKeyBindings(KeyBinding.PASTE, altZ);

        assertEquals(List.of(), conflicts);
    }

    // [utest->req~ux.keybindings.duplicate-warning~1]
    @Test
    void findsAllBindingsSharingTheCombination() {
        KeyBindingRepository repo = new KeyBindingRepository(
                List.of(KeyBinding.MERGE_ENTRIES.getConstant(), KeyBinding.NEW_TECHREPORT.getConstant(), KeyBinding.PASTE.getConstant()),
                List.of("alt+A", "alt+A", "shift+B"));
        KeyEvent altA = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.A, false, false, true, false);

        List<KeyBinding> conflicts = repo.findConflictingKeyBindings(KeyBinding.PASTE, altA);

        assertEquals(Set.of(KeyBinding.MERGE_ENTRIES, KeyBinding.NEW_TECHREPORT), Set.copyOf(conflicts));
    }

    // [utest->req~ux.keybindings.duplicate-warning~1]
    @Test
    void findsBindingsSharingEscape() {
        KeyBindingRepository repo = new KeyBindingRepository(
                List.of(KeyBinding.CLOSE.getConstant(), KeyBinding.CLEAR_SEARCH.getConstant(), KeyBinding.PASTE.getConstant()),
                List.of("Esc", "Esc", "shift+B"));
        KeyEvent escape = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false);

        List<KeyBinding> conflicts = repo.findConflictingKeyBindings(KeyBinding.PASTE, escape);

        assertEquals(Set.of(KeyBinding.CLOSE, KeyBinding.CLEAR_SEARCH), Set.copyOf(conflicts));
    }
}
